package com.dev.lms.course_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateSectionRequest(
        @NotBlank String title,
        @NotEmpty @Valid List<CreateLessonRequest> lessons
) {}
