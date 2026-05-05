package com.dev.lms.search_service.dto;

import java.time.Instant;

public record CourseHit(
        String courseId,
        String title,
        String description,
        String instructorName,
        String instructorId,
        String category,
        String subcategory,
        Float rating,
        Long ratingCount,
        Long enrollments,
        String language,
        Integer durationMinutes,
        Float price,
        Boolean isFree,
        String level,
        Instant createdAt,
        Float score
) {}
