package com.dev.lms.course_service.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record LessonResponse(
        UUID lessonId,
        UUID sectionId,
        String title,
        String lessonType,
        String videoUrl,
        String textUrl,
        Integer durationMinutes,
        Boolean isPreview,
        LocalDateTime createdAt
) {}
