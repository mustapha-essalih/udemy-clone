package com.dev.lms.auth_service.service;

import com.dev.lms.auth_service.dto.AuthResponse;
import com.dev.lms.auth_service.dto.LoginRequest;
import com.dev.lms.auth_service.dto.RefreshTokenRequest;
import com.dev.lms.auth_service.dto.RegisterRequest;
import com.dev.lms.auth_service.dto.RegistrationResponse;
import com.dev.lms.auth_service.entity.RefreshToken;
import com.dev.lms.auth_service.entity.Role;
import com.dev.lms.auth_service.entity.RoleName;
import com.dev.lms.auth_service.entity.User;
import com.dev.lms.auth_service.entity.UserStatus;
import com.dev.lms.auth_service.exception.ConflictException;
import com.dev.lms.auth_service.exception.ForbiddenException;
import com.dev.lms.auth_service.exception.UnauthorizedException;
import com.dev.lms.auth_service.mapper.UserMapper;
import com.dev.lms.auth_service.repository.RefreshTokenRepository;
import com.dev.lms.auth_service.repository.RoleRepository;
import com.dev.lms.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Transactional
    public RegistrationResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already in use");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Username already in use");
        }
        RoleName role = request.role() != null ? request.role() : RoleName.STUDENT;
        if (role != RoleName.STUDENT && role != RoleName.INSTRUCTOR) {
            throw new ForbiddenException("Can only register as student or instructor");
        }

        User user = userMapper.toEntity(request);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.ACTIVE);
        
        // Fetch existing role instead of creating it
        Role roleEntity = roleRepository.findByName(role)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        user.getRoles().add(roleEntity);

        User saved = userRepository.save(user);
        return buildRegistrationResponse(saved);

    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @Transactional
    public RegistrationResponse registerManager(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already in use");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Username already in use");
        }

        User user = userMapper.toEntity(request);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(new HashSet<>());
        user.getRoles().add(Role.builder().name(RoleName.MANAGER).build());
        User saved = userRepository.save(user);
        return buildRegistrationResponse(saved);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (AuthenticationException e) {
            throw new UnauthorizedException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(stored);
            throw new UnauthorizedException("Refresh token expired");
        }

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        String accessToken = jwtService.generateAccessToken(
                user.getId(), user.getEmail(), userMapper.primaryRole(user));

        return userMapper.toAuthResponse(user, accessToken, stored.getToken(), accessTokenExpiration / 1000);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(refreshTokenRepository::delete);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(
                user.getId(), user.getEmail(), userMapper.primaryRole(user));
        String refreshToken = rotateRefreshToken(user.getId());
        return userMapper.toAuthResponse(user, accessToken, refreshToken, accessTokenExpiration / 1000);
    }

    private RegistrationResponse buildRegistrationResponse(User user) {
        return userMapper.toRegistrationResponse(user);
    }

    private String rotateRefreshToken(UUID userId) {
        refreshTokenRepository.deleteByUserId(userId);

        String token = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .token(token)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                .build();
        refreshTokenRepository.save(refreshToken);
        return token;
    }
}
