package com.dev.lms.content_service.dto;

import java.util.List;

public record ResumeUploadResponse(
        String sessionId,
        String status,
        Integer uploadedChunks,
        Integer totalChunks,
        Integer nextChunkIndex,
        List<Integer> missingChunks
) {}
