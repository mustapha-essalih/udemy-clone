package com.dev.lms.course_service.dto;

public record LessonOverviewResponse(
        String lessonId,
        String title,
        String lessonType,
        String videoUrl,
        String textUrl,
        Integer durationMinutes,
        Boolean isPreview
) {}
