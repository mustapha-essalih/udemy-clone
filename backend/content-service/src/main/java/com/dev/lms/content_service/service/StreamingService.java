package com.dev.lms.content_service.service;

import com.dev.lms.content_service.entity.MediaFile;
import com.dev.lms.content_service.entity.StorageProvider;
import com.dev.lms.content_service.exception.BusinessException;
import com.dev.lms.content_service.exception.ResourceNotFoundException;
import com.dev.lms.content_service.repository.MediaFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class StreamingService {

    private final MediaFileRepository mediaFileRepository;

    @Autowired(required = false)
    private S3AsyncClient s3AsyncClient;

    @Value("${content.storage.s3.bucket-name:#{null}}")
    private String bucketName;

    private static final int BUFFER_SIZE = 256 * 1024;
    private static final DataBufferFactory BUFFER_FACTORY = new DefaultDataBufferFactory();

    public Mono<ResponseEntity<Flux<DataBuffer>>> stream(UUID mediaId, String rangeHeader) {
        return Mono.fromCallable(() ->
                        mediaFileRepository.findById(mediaId)
                                .orElseThrow(() -> new ResourceNotFoundException("Media", mediaId)))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(media -> media.getStorageProvider() == StorageProvider.S3
                        ? streamS3(media, rangeHeader)
                        : streamLocal(media, rangeHeader));
    }

    private Mono<ResponseEntity<Flux<DataBuffer>>> streamLocal(MediaFile media, String rangeHeader) {
        if (media.getLocalPath() == null) {
            return Mono.error(new BusinessException("No local path for media: " + media.getId()));
        }
        Path path = Path.of(media.getLocalPath());
        return Mono.fromCallable(() -> Files.size(path))
                .subscribeOn(Schedulers.boundedElastic())
                .map(fileSize -> {
                    long start = 0;
                    long end = fileSize - 1;
                    boolean isRange = rangeHeader != null && rangeHeader.startsWith("bytes=");

                    if (isRange) {
                        String[] parts = rangeHeader.substring(6).split("-");
                        start = Long.parseLong(parts[0].trim());
                        end = (parts.length > 1 && !parts[1].trim().isEmpty())
                                ? Long.parseLong(parts[1].trim())
                                : fileSize - 1;
                    }

                    long clampedEnd = Math.min(end, fileSize - 1);
                    long contentLength = clampedEnd - start + 1;
                    final long finalStart = start;
                    final long finalEnd = clampedEnd;

                    Flux<DataBuffer> body = DataBufferUtils
                            .readAsynchronousFileChannel(
                                    () -> AsynchronousFileChannel.open(path, StandardOpenOption.READ),
                                    finalStart,
                                    BUFFER_FACTORY,
                                    BUFFER_SIZE)
                            .transform(flux -> trimToLength(flux, contentLength));

                    String mimeType = media.getMimeType() != null ? media.getMimeType() : "application/octet-stream";

                    ResponseEntity.BodyBuilder builder = ResponseEntity
                            .status(isRange ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK)
                            .contentType(MediaType.parseMediaType(mimeType))
                            .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                            .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength));

                    if (isRange) {
                        builder.header(HttpHeaders.CONTENT_RANGE,
                                String.format("bytes %d-%d/%d", finalStart, finalEnd, fileSize));
                    }

                    return builder.body(body);
                });
    }

    private Mono<ResponseEntity<Flux<DataBuffer>>> streamS3(MediaFile media, String rangeHeader) {
        if (s3AsyncClient == null) {
            return Mono.error(new BusinessException("S3 client not configured — enable prod profile"));
        }

        GetObjectRequest.Builder reqBuilder = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(media.getStorageKey());

        if (rangeHeader != null) {
            reqBuilder.range(rangeHeader);
        }

        return Mono.fromFuture(
                s3AsyncClient.getObject(reqBuilder.build(), AsyncResponseTransformer.toPublisher())
        ).map(responsePublisher -> {
            GetObjectResponse s3Meta = responsePublisher.response();

            Flux<DataBuffer> body = Flux.from(responsePublisher)
                    .map(byteBuffer -> {
                        DataBuffer buf = BUFFER_FACTORY.allocateBuffer(byteBuffer.remaining());
                        buf.write(byteBuffer);
                        return buf;
                    });

            String contentType = s3Meta.contentType() != null
                    ? s3Meta.contentType() : "application/octet-stream";

            ResponseEntity.BodyBuilder builder = ResponseEntity
                    .status(rangeHeader != null ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK)
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes");

            if (s3Meta.contentLength() != null) {
                builder.header(HttpHeaders.CONTENT_LENGTH, String.valueOf(s3Meta.contentLength()));
            }
            if (s3Meta.contentRange() != null) {
                builder.header(HttpHeaders.CONTENT_RANGE, s3Meta.contentRange());
            }

            return builder.body(body);
        });
    }

    private Flux<DataBuffer> trimToLength(Flux<DataBuffer> source, long maxBytes) {
        AtomicLong remaining = new AtomicLong(maxBytes);
        return source.concatMap(buffer -> {
            long rem = remaining.get();
            if (rem <= 0) {
                DataBufferUtils.release(buffer);
                return Mono.empty();
            }
            int readable = buffer.readableByteCount();
            if (readable <= rem) {
                remaining.addAndGet(-readable);
                return Mono.just(buffer);
            }
            int keep = (int) rem;
            remaining.set(0);
            byte[] bytes = new byte[keep];
            buffer.read(bytes);
            DataBufferUtils.release(buffer);
            DataBuffer slice = BUFFER_FACTORY.allocateBuffer(keep);
            slice.write(bytes);
            return Mono.just(slice);
        });
    }
}
