package com.dev.lms.auth_service.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        String userId,
        String email,
        String role
) {}
