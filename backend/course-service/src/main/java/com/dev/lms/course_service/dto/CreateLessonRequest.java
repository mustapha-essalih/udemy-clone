package com.dev.lms.course_service.dto;

import com.dev.lms.course_service.entity.LessonType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateLessonRequest(
        @NotBlank String title,
        @NotNull LessonType lessonType
) {}
