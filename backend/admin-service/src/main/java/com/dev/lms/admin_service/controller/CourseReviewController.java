package com.dev.lms.admin_service.controller;

import com.dev.lms.admin_service.client.CourseServiceClient;
import com.dev.lms.admin_service.dto.CourseDraftDto;
import com.dev.lms.admin_service.dto.ReviewDecisionRequest;
import com.dev.lms.admin_service.entity.CourseReviewQueue;
import com.dev.lms.admin_service.entity.ReviewQueueStatus;
import com.dev.lms.admin_service.repository.CourseReviewQueueRepository;
import com.dev.lms.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/courses")
@RequiredArgsConstructor
public class CourseReviewController {

    private final CourseServiceClient courseServiceClient;
    private final CourseReviewQueueRepository reviewQueueRepository;

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<CourseDraftDto>>> getPendingCourses() {
        return ResponseEntity.ok(ApiResponse.ok(courseServiceClient.getPendingDrafts()));
    }

    @GetMapping("/drafts/{draftId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CourseDraftDto>> getDraft(@PathVariable String draftId) {
        return ResponseEntity.ok(ApiResponse.ok(courseServiceClient.getDraft(draftId)));
    }

    @PostMapping("/{draftId}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Object>> approve(
            @PathVariable String draftId,
            @RequestHeader("X-User-Id") String reviewerId) {
        Object course = courseServiceClient.approveDraft(draftId);

        reviewQueueRepository.save(CourseReviewQueue.builder()
                .courseId(UUID.fromString(draftId))
                .status(ReviewQueueStatus.APPROVED)
                .reviewedBy(UUID.fromString(reviewerId))
                .reviewedAt(LocalDateTime.now())
                .build());

        return ResponseEntity.ok(ApiResponse.ok("Course approved and published", course));
    }

    @PostMapping("/{draftId}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CourseDraftDto>> reject(
            @PathVariable String draftId,
            @Valid @RequestBody ReviewDecisionRequest request,
            @RequestHeader("X-User-Id") String reviewerId) {
        CourseDraftDto draft = courseServiceClient.rejectDraft(draftId, request.feedback());

        reviewQueueRepository.save(CourseReviewQueue.builder()
                .courseId(UUID.fromString(draftId))
                .status(ReviewQueueStatus.REJECTED)
                .feedback(request.feedback())
                .reviewedBy(UUID.fromString(reviewerId))
                .reviewedAt(LocalDateTime.now())
                .build());

        return ResponseEntity.ok(ApiResponse.ok("Course rejected", draft));
    }
}
