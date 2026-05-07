package com.dev.lms.payment_service.dto;

import com.dev.lms.payment_service.entity.EnrollmentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record EnrollmentResponse(
        UUID id,
        UUID userId,
        UUID courseId,
        UUID paymentId,
        EnrollmentStatus status,
        LocalDateTime enrolledAt
) {}
