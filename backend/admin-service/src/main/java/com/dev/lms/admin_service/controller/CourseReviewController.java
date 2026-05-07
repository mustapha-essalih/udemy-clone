package com.dev.lms.admin_service.controller;

import com.dev.lms.admin_service.dto.CourseDraftDto;
import com.dev.lms.admin_service.dto.ReviewDecisionRequest;
import com.dev.lms.admin_service.service.CourseReviewService;
import com.dev.lms.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/courses")
@RequiredArgsConstructor
public class CourseReviewController {

    private final CourseReviewService courseReviewService;

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<CourseDraftDto>>> getPendingCourses() {
        return ResponseEntity.ok(ApiResponse.ok(courseReviewService.getPendingCourses()));
    }

    @GetMapping("/drafts/{draftId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CourseDraftDto>> getDraft(@PathVariable String draftId) {
        return ResponseEntity.ok(ApiResponse.ok(courseReviewService.getDraft(draftId)));
    }

    @PostMapping("/{draftId}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Object>> approve(
            @PathVariable String draftId,
            @RequestHeader("X-User-Id") String reviewerId) {
        Object course = courseReviewService.approve(draftId, UUID.fromString(reviewerId));
        return ResponseEntity.ok(ApiResponse.ok("Course approved and published", course));
    }

    @PostMapping("/{draftId}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CourseDraftDto>> reject(
            @PathVariable String draftId,
            @Valid @RequestBody ReviewDecisionRequest request,
            @RequestHeader("X-User-Id") String reviewerId) {
        CourseDraftDto draft = courseReviewService.reject(draftId, request.feedback(), UUID.fromString(reviewerId));
        return ResponseEntity.ok(ApiResponse.ok("Course rejected", draft));
    }
}
