package com.dev.lms.course_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CourseResponse(
        UUID courseId,
        UUID instructorId,
        String title,
        String subTitle,
        String description,
        BigDecimal price,
        Boolean isFree,
        String language,
        String couponCode,
        BigDecimal rating,
        Integer courseDurationMinutes,
        String status,
        String level,
        List<SectionResponse> sections,
        LocalDateTime createdAt
) {}
