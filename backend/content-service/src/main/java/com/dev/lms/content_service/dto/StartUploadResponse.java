package com.dev.lms.content_service.dto;

public record StartUploadResponse(
        String sessionId,
        Integer totalChunks,
        Integer uploadedChunks,
        Integer nextChunkIndex
) {}
