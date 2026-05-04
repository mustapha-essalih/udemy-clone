package com.dev.lms.content_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record StartUploadRequest(
        @NotNull UUID ownerId,
        @NotNull UUID courseId,
        @NotNull UUID sectionId,
        @NotNull UUID lessonId,
        @NotBlank String courseName,
        @NotBlank String sectionName,
        @NotBlank String lessonTitle,
        @NotBlank String fileName,
        String mimeType,
        @NotNull @Positive Long totalSize,
        @NotNull @Positive Long chunkSize
) {}
