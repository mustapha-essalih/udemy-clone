package com.dev.lms.course_service.dto;

import java.util.List;

public record SectionOverviewResponse(
        String sectionId,
        String title,
        List<LessonOverviewResponse> lessons
) {}
