package com.dev.lms.content_service.dto;

public record CompleteUploadResponse(
        String mediaId,
        String url,
        String localPath,
        String fileName,
        String fileType,
        String mimeType,
        Long size
) {}
