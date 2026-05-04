package com.dev.lms.content_service.dto;

public record UploadProgressResponse(
        String sessionId,
        String status,
        Integer uploadedChunks,
        Integer totalChunks,
        Long uploadedBytes,
        Long totalSize,
        Double percentComplete
) {}
