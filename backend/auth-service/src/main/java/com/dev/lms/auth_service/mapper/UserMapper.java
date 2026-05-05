package com.dev.lms.auth_service.mapper;

import com.dev.lms.auth_service.dto.AuthResponse;
import com.dev.lms.auth_service.entity.User;
import com.dev.lms.common.request.RegisterRequest;
import com.dev.lms.common.response.RegistrationResponse;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    User toEntity(RegisterRequest request);

    @Mapping(target = "userId", source = "user.id", qualifiedByName = "uuidToString")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "role", expression = "java(primaryRole(user))")
    RegistrationResponse toRegistrationResponse(User user);

    @Mapping(target = "userId", source = "user.id", qualifiedByName = "uuidToString")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "role", expression = "java(primaryRole(user))")
    @Mapping(target = "tokenType", constant = "Bearer")
    @Mapping(target = "accessToken", source = "accessToken")
    @Mapping(target = "refreshToken", source = "refreshToken")
    @Mapping(target = "expiresIn", source = "expiresIn")
    AuthResponse toAuthResponse(User user, String accessToken, String refreshToken, Long expiresIn);

    @Named("uuidToString")
    default String uuidToString(UUID uuid) {
        return uuid != null ? uuid.toString() : null;
    }

    default String primaryRole(User user) {
        return user.getRoles().stream()
                .findFirst()
                .map(r -> r.getName().name())
                .orElse("STUDENT");
    }
}
