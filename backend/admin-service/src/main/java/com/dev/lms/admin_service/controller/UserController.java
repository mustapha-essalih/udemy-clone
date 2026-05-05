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



    // @GetMapping
    // public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
    //     List<UserResponse> users = userService.getAllUsers();
    //     return ResponseEntity.ok(ApiResponse.ok("Users fetched successfully", users));
    // }


    // crud on manager

    @PostMapping("/manager")
    public ResponseEntity<ApiResponse<RegistrationResponse>> createManager(@Valid @RequestBody RegisterRequest request) {
        RegistrationResponse manager = userService.createManager(request);
        return ResponseEntity.ok(ApiResponse.ok("Manager created successfully", manager));
    }

    // @GetMapping("/manager/{id}")
    // public ResponseEntity<ApiResponse<UserResponse>> getManagerById(@PathVariable UUID id) {
    //     UserResponse manager = userService.getManagerById(id);
    //     return ResponseEntity.ok(ApiResponse.ok("Manager fetched successfully", manager));
    // }

    // @PutMapping("/manager/{id}")
    // public ResponseEntity<ApiResponse<UserResponse>> updateManager(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
    //     UserResponse manager = userService.updateManager(id, request);
    //     return ResponseEntity.ok(ApiResponse.ok("Manager updated successfully", manager));
    // }

    // @DeleteMapping("/manager/{id}")
    // public ResponseEntity<ApiResponse<Void>> deleteManager(@PathVariable UUID id) {
    //     userService.deleteManager(id);
    //     return ResponseEntity.ok(ApiResponse.ok("Manager deleted successfully"));
    // }


    // get all users

    // @GetMapping("/all")
    // public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
    //     List<UserResponse> users = userService.getAllUsers();
    //     return ResponseEntity.ok(ApiResponse.ok("Users fetched successfully", users));
    // }   

    // // delete user

    // @DeleteMapping("/{id}")
    // public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID id) {
    //     userService.deleteUser(id);
    //     return ResponseEntity.ok(ApiResponse.ok("User deleted successfully"));
    // }
    
}
