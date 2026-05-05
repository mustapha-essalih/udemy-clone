package com.dev.lms.admin_service.dto;

import jakarta.validation.constraints.NotBlank;

public record ReviewDecisionRequest(@NotBlank String feedback) {}
