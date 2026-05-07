package com.dev.lms.payment_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LessonProgressRequest(
        @NotNull UUID lessonId,
        boolean completed,
        @Min(0) @Max(100) Integer progressPercent,
        @Min(0) Integer lastPositionSeconds
) {}
