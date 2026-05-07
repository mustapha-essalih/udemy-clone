package com.dev.lms.course_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CourseInfoResponse(
        UUID courseId,
        UUID instructorId,
        String title,
        BigDecimal price,
        Boolean isFree,
        String status
) {}
