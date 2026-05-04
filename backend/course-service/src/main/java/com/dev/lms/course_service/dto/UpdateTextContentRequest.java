package com.dev.lms.course_service.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateTextContentRequest(
        @NotBlank String contentUrl
) {}
