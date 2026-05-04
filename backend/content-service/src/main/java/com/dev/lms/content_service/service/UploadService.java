package com.dev.lms.content_service.service;

import com.dev.lms.content_service.dto.*;
import com.dev.lms.content_service.entity.*;
import com.dev.lms.content_service.exception.BusinessException;
import com.dev.lms.content_service.exception.ResourceNotFoundException;
import com.dev.lms.content_service.repository.MediaFileRepository;
import com.dev.lms.content_service.repository.UploadSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadService {

    private final UploadSessionRepository sessionRepository;
    private final MediaFileRepository mediaFileRepository;

    @Value("${content.storage.local.base-path:/home/ahmed-yassine/Desktop/udemy-clone/backend/uploads}")
    private String basePath;

    @Value("${content.storage.local.base-url:http://localhost:8084}")
    private String baseUrl;

    public Mono<StartUploadResponse> startUpload(StartUploadRequest req) {
        return Mono.fromCallable(() -> {
            int totalChunks = (int) Math.ceil((double) req.totalSize() / req.chunkSize());
            String tempPath = Paths.get(basePath, "temp", UUID.randomUUID().toString())
                    .toAbsolutePath().toString();
            Files.createDirectories(Paths.get(tempPath, "chunks"));

            UploadSession session = UploadSession.builder()
                    .ownerId(req.ownerId())
                    .courseId(req.courseId())
                    .sectionId(req.sectionId())
                    .lessonId(req.lessonId())
                    .courseName(req.courseName())
                    .sectionName(req.sectionName())
                    .lessonTitle(req.lessonTitle())
                    .fileName(req.fileName())
                    .mimeType(req.mimeType())
                    .totalSize(req.totalSize())
                    .chunkSize(req.chunkSize())
                    .totalChunks(totalChunks)
                    .tempPath(tempPath)
                    .build();

            UploadSession saved = sessionRepository.save(session);
            return new StartUploadResponse(saved.getId().toString(), totalChunks, 0, 0);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<UploadProgressResponse> uploadChunk(UUID sessionId, int chunkIndex, FilePart data) {
        return Mono.fromCallable(() -> sessionRepository.findById(sessionId)
                        .orElseThrow(() -> new ResourceNotFoundException("UploadSession", sessionId)))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(session -> {
                    validateChunkUpload(session, chunkIndex);
                    Path chunkPath = Paths.get(session.getTempPath(), "chunks",
                            String.format("chunk_%05d", chunkIndex));
                    long chunkBytes = Math.min(session.getChunkSize(),
                            session.getTotalSize() - (long) chunkIndex * session.getChunkSize());
                    return Mono.fromCallable(() -> {
                                Files.createDirectories(chunkPath.getParent());
                                return chunkPath;
                            })
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(path -> data.transferTo(path)
                                    .onErrorMap(IOException.class,
                                            e -> new BusinessException("Failed to write chunk " + chunkIndex + ": " + e.getMessage()))
                                    .then(Mono.fromCallable(() -> {
                                        sessionRepository.incrementProgress(sessionId, chunkBytes, UploadStatus.IN_PROGRESS);
                                        long newBytes = session.getUploadedBytes() + chunkBytes;
                                        int newChunks = session.getUploadedChunks() + 1;
                                        double pct = session.getTotalSize() > 0
                                                ? Math.round((double) newBytes / session.getTotalSize() * 10000.0) / 100.0
                                                : 0.0;
                                        return new UploadProgressResponse(
                                                sessionId.toString(), UploadStatus.IN_PROGRESS.name(),
                                                newChunks, session.getTotalChunks(),
                                                newBytes, session.getTotalSize(), pct);
                                    }).subscribeOn(Schedulers.boundedElastic())));
                });
    }

    public Mono<CompleteUploadResponse> completeUpload(UUID sessionId) {
        return Mono.fromCallable(() -> sessionRepository.findById(sessionId)
                        .orElseThrow(() -> new ResourceNotFoundException("UploadSession", sessionId)))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(session -> {
                    if (session.getStatus() == UploadStatus.COMPLETED) {
                        MediaFile existing = mediaFileRepository.findById(session.getMediaId())
                                .orElseThrow(() -> new BusinessException("Media file missing for completed session"));
                        return Mono.just(toCompleteResponse(existing));
                    }
                    int uploaded = countUploadedChunks(session.getTempPath(), session.getTotalChunks());
                    if (uploaded < session.getTotalChunks()) {
                        throw new BusinessException(String.format(
                                "Incomplete upload: %d of %d chunks received", uploaded, session.getTotalChunks()));
                    }
                    return Mono.fromCallable(() -> mergeAndFinalize(session))
                            .subscribeOn(Schedulers.boundedElastic());
                });
    }

    public Mono<UploadProgressResponse> pauseUpload(UUID sessionId) {
        return Mono.fromCallable(() -> {
            UploadSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new ResourceNotFoundException("UploadSession", sessionId));
            if (session.getStatus() != UploadStatus.IN_PROGRESS && session.getStatus() != UploadStatus.PENDING) {
                throw new BusinessException("Cannot pause upload in status: " + session.getStatus());
            }
            session.setStatus(UploadStatus.PAUSED);
            return toProgressResponse(sessionRepository.save(session));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<ResumeUploadResponse> resumeUpload(UUID sessionId) {
        return Mono.fromCallable(() -> {
            UploadSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new ResourceNotFoundException("UploadSession", sessionId));
            if (session.getStatus() != UploadStatus.PAUSED) {
                throw new BusinessException("Cannot resume upload in status: " + session.getStatus());
            }
            List<Integer> missing = findMissingChunks(session.getTempPath(), session.getTotalChunks());
            int uploadedChunks = session.getTotalChunks() - missing.size();
            session.setStatus(UploadStatus.IN_PROGRESS);
            session.setUploadedChunks(uploadedChunks);
            sessionRepository.save(session);

            int nextIndex = missing.isEmpty() ? session.getTotalChunks() : missing.get(0);
            return new ResumeUploadResponse(
                    sessionId.toString(), UploadStatus.IN_PROGRESS.name(),
                    uploadedChunks, session.getTotalChunks(), nextIndex, missing
            );
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> cancelUpload(UUID sessionId) {
        return Mono.fromCallable(() -> {
            UploadSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new ResourceNotFoundException("UploadSession", sessionId));
            if (session.getStatus() == UploadStatus.COMPLETED) {
                throw new BusinessException("Cannot cancel a completed upload");
            }
            session.setStatus(UploadStatus.CANCELLED);
            sessionRepository.save(session);
            if (session.getTempPath() != null) {
                deleteDirectory(Paths.get(session.getTempPath()));
            }
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    public Mono<UploadProgressResponse> getProgress(UUID sessionId) {
        return Mono.fromCallable(() -> {
            UploadSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new ResourceNotFoundException("UploadSession", sessionId));
            return toProgressResponse(session);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private String sanitizeName(String name) {
        if (name == null || name.isBlank()) return "unknown";
        String result = name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        if (result.isEmpty()) return "unknown";
        return result.length() > 50 ? result.substring(0, 50) : result;
    }

    private CompleteUploadResponse mergeAndFinalize(UploadSession session) throws IOException {
        String courseFolder = (session.getCourseName() != null)
                ? sanitizeName(session.getCourseName()) + "-" + session.getCourseId()
                : session.getCourseId().toString();
        String sectionFolder = (session.getSectionName() != null)
                ? sanitizeName(session.getSectionName())
                : session.getSectionId().toString();
        String lessonFolder = (session.getLessonTitle() != null)
                ? sanitizeName(session.getLessonTitle())
                : session.getLessonId().toString();

        Path finalDir = Paths.get(basePath, "courses", courseFolder,
                "sections", sectionFolder,
                "lessons", lessonFolder).toAbsolutePath();
        Files.createDirectories(finalDir);

        Path finalPath = finalDir.resolve(session.getFileName());
        mergeChunks(session.getTempPath(), session.getTotalChunks(), finalPath);

        long fileSize = Files.size(finalPath);
        Path baseAbsolute = Paths.get(basePath).toAbsolutePath();
        String relativePath = baseAbsolute.relativize(finalPath).toString().replace("\\", "/");
        String url = baseUrl + "/api/content/files/" + relativePath;

        MediaFile media = MediaFile.builder()
                .ownerId(session.getOwnerId())
                .courseId(session.getCourseId())
                .sectionId(session.getSectionId())
                .lessonId(session.getLessonId())
                .fileName(session.getFileName())
                .originalName(session.getFileName())
                .fileType(detectFileType(session.getMimeType()))
                .mimeType(session.getMimeType())
                .size(fileSize)
                .localPath(finalPath.toString())
                .url(url)
                .build();

        MediaFile savedMedia = mediaFileRepository.save(media);

        session.setMediaId(savedMedia.getId());
        session.setStatus(UploadStatus.COMPLETED);
        session.setUploadedBytes(fileSize);
        session.setUploadedChunks(session.getTotalChunks());
        sessionRepository.save(session);

        deleteDirectory(Paths.get(session.getTempPath()));

        return toCompleteResponse(savedMedia);
    }

    private void validateChunkUpload(UploadSession session, int chunkIndex) {
        if (session.getStatus() == UploadStatus.COMPLETED
                || session.getStatus() == UploadStatus.CANCELLED
                || session.getStatus() == UploadStatus.FAILED) {
            throw new BusinessException("Cannot upload chunk to session in status: " + session.getStatus());
        }
        if (chunkIndex < 0 || chunkIndex >= session.getTotalChunks()) {
            throw new BusinessException("Invalid chunk index: " + chunkIndex +
                    " (valid range: 0 to " + (session.getTotalChunks() - 1) + ")");
        }
    }

    private int countUploadedChunks(String tempPath, int totalChunks) {
        int count = 0;
        for (int i = 0; i < totalChunks; i++) {
            if (Files.exists(Paths.get(tempPath, "chunks", String.format("chunk_%05d", i)))) count++;
        }
        return count;
    }

    private List<Integer> findMissingChunks(String tempPath, int totalChunks) {
        List<Integer> missing = new ArrayList<>();
        for (int i = 0; i < totalChunks; i++) {
            if (!Files.exists(Paths.get(tempPath, "chunks", String.format("chunk_%05d", i)))) {
                missing.add(i);
            }
        }
        return missing;
    }

    private void mergeChunks(String tempPath, int totalChunks, Path destination) throws IOException {
        try (FileChannel out = FileChannel.open(destination,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (int i = 0; i < totalChunks; i++) {
                Path chunkPath = Paths.get(tempPath, "chunks", String.format("chunk_%05d", i));
                try (FileChannel in = FileChannel.open(chunkPath, StandardOpenOption.READ)) {
                    long size = in.size();
                    long transferred = 0;
                    while (transferred < size) {
                        transferred += in.transferTo(transferred, size - transferred, out);
                    }
                }
            }
        }
    }

    private void deleteDirectory(Path dir) {
        try {
            if (Files.exists(dir)) {
                Files.walk(dir).sorted(Comparator.reverseOrder()).forEach(p -> {
                    try { Files.delete(p); } catch (IOException e) { log.warn("Failed to delete {}", p); }
                });
            }
        } catch (IOException e) {
            log.warn("Failed to delete directory {}", dir);
        }
    }

    private FileType detectFileType(String mimeType) {
        if (mimeType == null) return FileType.DOCUMENT;
        if (mimeType.startsWith("video/")) return FileType.VIDEO;
        if (mimeType.startsWith("image/")) return FileType.IMAGE;
        if (mimeType.startsWith("audio/")) return FileType.AUDIO;
        return FileType.DOCUMENT;
    }

    private UploadProgressResponse toProgressResponse(UploadSession s) {
        double pct = s.getTotalSize() > 0
                ? Math.round((double) s.getUploadedBytes() / s.getTotalSize() * 10000.0) / 100.0
                : 0.0;
        return new UploadProgressResponse(s.getId().toString(), s.getStatus().name(),
                s.getUploadedChunks(), s.getTotalChunks(),
                s.getUploadedBytes(), s.getTotalSize(), pct);
    }

    private CompleteUploadResponse toCompleteResponse(MediaFile m) {
        return new CompleteUploadResponse(m.getId().toString(), m.getUrl(), m.getLocalPath(),
                m.getFileName(), m.getFileType() != null ? m.getFileType().name() : null,
                m.getMimeType(), m.getSize());
    }
}
