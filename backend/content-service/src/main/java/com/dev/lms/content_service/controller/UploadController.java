package com.dev.lms.content_service.controller;

import com.dev.lms.common.response.ApiResponse;
import com.dev.lms.content_service.dto.*;
import com.dev.lms.content_service.service.UploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/content/upload")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<StartUploadResponse>> startUpload(@Valid @RequestBody StartUploadRequest request) {
        return uploadService.startUpload(request)
                .map(r -> ApiResponse.ok("Upload session created", r));
    }

    @PostMapping(value = "/{sessionId}/chunk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ApiResponse<UploadProgressResponse>> uploadChunk(
            @PathVariable UUID sessionId,
            @RequestPart("chunkIndex") String chunkIndex,
            @RequestPart("data") FilePart data) {
        return uploadService.uploadChunk(sessionId, Integer.parseInt(chunkIndex), data)
                .map(r -> ApiResponse.ok("Chunk uploaded", r));
    }

    @PostMapping("/{sessionId}/complete")
    public Mono<ApiResponse<CompleteUploadResponse>> completeUpload(@PathVariable UUID sessionId) {
        return uploadService.completeUpload(sessionId)
                .map(r -> ApiResponse.ok("Upload completed", r));
    }

    @PutMapping("/{sessionId}/pause")
    public Mono<ApiResponse<UploadProgressResponse>> pauseUpload(@PathVariable UUID sessionId) {
        return uploadService.pauseUpload(sessionId)
                .map(r -> ApiResponse.ok("Upload paused", r));
    }

    @PutMapping("/{sessionId}/resume")
    public Mono<ApiResponse<ResumeUploadResponse>> resumeUpload(@PathVariable UUID sessionId) {
        return uploadService.resumeUpload(sessionId)
                .map(r -> ApiResponse.ok("Upload resumed", r));
    }

    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> cancelUpload(@PathVariable UUID sessionId) {
        return uploadService.cancelUpload(sessionId);
    }

    @GetMapping("/{sessionId}/progress")
    public Mono<ApiResponse<UploadProgressResponse>> getProgress(@PathVariable UUID sessionId) {
        return uploadService.getProgress(sessionId)
                .map(ApiResponse::ok);
    }
}
