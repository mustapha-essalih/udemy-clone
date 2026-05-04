package com.dev.lms.content_service.dto;

import java.util.UUID;

public record MediaResponse(
        String mediaId,
        UUID courseId,
        UUID sectionId,
        UUID lessonId,
        String url,
        String fileName,
        String fileType,
        String mimeType,
        Long size,
        String status
) {}
