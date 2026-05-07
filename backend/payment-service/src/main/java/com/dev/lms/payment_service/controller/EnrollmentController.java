package com.dev.lms.payment_service.controller;

import com.dev.lms.common.response.ApiResponse;
import com.dev.lms.payment_service.dto.EnrollmentResponse;
import com.dev.lms.payment_service.dto.LessonProgressRequest;
import com.dev.lms.payment_service.entity.LessonProgress;
import com.dev.lms.payment_service.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<EnrollmentResponse>>> getMyEnrollments(
            @RequestHeader("X-User-Id") String userId) {
        List<EnrollmentResponse> enrollments = enrollmentService.getEnrollmentsByUser(UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.ok(enrollments));
    }

    @GetMapping("/courses/{courseId}/check")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Boolean>> checkEnrollment(
            @PathVariable UUID courseId,
            @RequestHeader("X-User-Id") String userId) {
        boolean enrolled = enrollmentService.isEnrolled(UUID.fromString(userId), courseId);
        return ResponseEntity.ok(ApiResponse.ok(enrolled));
    }

    @GetMapping("/{enrollmentId}/progress")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<LessonProgress>>> getLessonProgress(
            @PathVariable UUID enrollmentId,
            @RequestHeader("X-User-Id") String userId) {
        List<LessonProgress> progress = enrollmentService.getLessonProgress(enrollmentId, UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.ok(progress));
    }

    @PutMapping("/{enrollmentId}/progress")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<LessonProgress>> updateLessonProgress(
            @PathVariable UUID enrollmentId,
            @Valid @RequestBody LessonProgressRequest request,
            @RequestHeader("X-User-Id") String userId) {
        LessonProgress progress = enrollmentService.upsertLessonProgress(enrollmentId, request, UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.ok(progress));
    }
}
