package com.dev.lms.course_service.dto;

import java.util.List;
import java.util.UUID;

public record SectionResponse(
        UUID sectionId,
        UUID courseId,
        String title,
        List<LessonResponse> lessons
) {}
