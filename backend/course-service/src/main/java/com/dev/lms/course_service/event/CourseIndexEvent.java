package com.dev.lms.course_service.event;

import java.time.Instant;
import java.util.List;

public record CourseIndexEvent(
        String eventType,
        String courseId,
        String title,
        String description,
        String instructorId,
        String instructorName,
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
        String status,
        Instant createdAt,
        Instant updatedAt,
        List<String> tags
) {
    public static final String CREATED = "CourseCreated";
    public static final String UPDATED = "CourseUpdated";
    public static final String DELETED = "CourseDeleted";
}
