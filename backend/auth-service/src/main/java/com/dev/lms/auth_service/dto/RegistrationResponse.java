package com.dev.lms.auth_service.dto;

public record RegistrationResponse(
    String userId,
    String username,
    String email,
    String role
) {
}
