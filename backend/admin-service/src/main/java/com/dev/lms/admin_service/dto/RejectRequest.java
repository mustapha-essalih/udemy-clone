package com.dev.lms.admin_service.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectRequest(
    @NotBlank(message = "Feedback is required")
    String feedback
) {}
