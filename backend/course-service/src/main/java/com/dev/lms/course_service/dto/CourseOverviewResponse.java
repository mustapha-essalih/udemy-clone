package com.dev.lms.course_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CourseOverviewResponse(
        String id,
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
        String level,
        String status,
        String source,
        String rejectionFeedback,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt,
        List<SectionOverviewResponse> sections
) {}
