package com.dev.lms.course_service.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectDraftRequest(@NotBlank String feedback) {}
