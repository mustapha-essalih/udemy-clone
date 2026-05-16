package com.dev.lms.admin_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dev.lms.admin_service.service.UserService;
import com.dev.lms.common.request.RegisterRequest;
import com.dev.lms.common.response.ApiResponse;
import com.dev.lms.common.response.RegistrationResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/manager")
    public ResponseEntity<ApiResponse<RegistrationResponse>> createManager(@Valid @RequestBody RegisterRequest request) {
        RegistrationResponse manager = userService.createManager(request);
        return ResponseEntity.ok(ApiResponse.ok("Manager created successfully", manager));
    }

    
}
