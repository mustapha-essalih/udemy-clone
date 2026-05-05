package com.dev.lms.common.response;

public record RegistrationResponse(
    String userId,
    String username,
    String email,
    String role
) {
}
