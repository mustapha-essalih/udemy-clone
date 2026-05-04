package com.dev.lms.course_service.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateVideoContentRequest(
        @NotBlank String videoUrl,
        Integer durationMinutes,
        Boolean isPreview
) {}
