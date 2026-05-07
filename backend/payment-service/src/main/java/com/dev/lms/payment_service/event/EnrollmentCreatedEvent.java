package com.dev.lms.payment_service.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record EnrollmentCreatedEvent(
        UUID enrollmentId,
        UUID userId,
        UUID courseId,
        UUID instructorId,
        LocalDateTime enrolledAt
) {}
