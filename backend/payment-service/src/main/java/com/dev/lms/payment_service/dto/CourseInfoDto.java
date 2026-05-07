package com.dev.lms.payment_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CourseInfoDto(
        UUID courseId,
        UUID instructorId,
        String title,
        BigDecimal price,
        Boolean isFree,
        String status
) {}
