package com.dev.lms.course_service.controller;

import com.dev.lms.common.response.ApiResponse;
import com.dev.lms.course_service.dto.DraftResponse;
import com.dev.lms.course_service.dto.RejectDraftRequest;
import com.dev.lms.course_service.dto.UpdateDraftRequest;
import com.dev.lms.course_service.dto.UpdateVideoContentRequest;
import com.dev.lms.course_service.dto.UpdateTextContentRequest;
import com.dev.lms.course_service.service.CourseDraftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses/drafts")
@RequiredArgsConstructor
public class CourseDraftController {

    private final CourseDraftService draftService;

    @GetMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<DraftResponse>>> listByInstructor(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.ok(draftService.listByInstructor(UUID.fromString(userId))));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<DraftResponse>>> listPending() {
        return ResponseEntity.ok(ApiResponse.ok(draftService.listPending()));
    }

    @GetMapping("/{draftId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DraftResponse>> getById(@PathVariable String draftId) {
        return ResponseEntity.ok(ApiResponse.ok(draftService.getById(draftId)));
    }

    @PutMapping("/{draftId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<DraftResponse>> update(
            @PathVariable String draftId,
            @Valid @RequestBody UpdateDraftRequest request,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.ok("Draft updated", draftService.update(draftId, UUID.fromString(userId), request)));
    }

    @PostMapping("/{draftId}/submit")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<DraftResponse>> submit(
            @PathVariable String draftId,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.ok("Course submitted for review", draftService.submit(draftId, UUID.fromString(userId))));
    }

    @PostMapping("/{draftId}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Object>> approve(@PathVariable String draftId) {
        return ResponseEntity.ok(ApiResponse.ok("Course approved and published", (Object) draftService.approve(draftId)));
    }

    @PostMapping("/{draftId}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DraftResponse>> reject(
            @PathVariable String draftId,
            @Valid @RequestBody RejectDraftRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Course rejected", draftService.reject(draftId, request.feedback())));
    }

    @PutMapping("/{draftId}/sections/{sectionId}/lessons/{lessonId}/video-url")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<DraftResponse>> updateVideoUrl(
            @PathVariable String draftId,
            @PathVariable String sectionId,
            @PathVariable String lessonId,
            @Valid @RequestBody UpdateVideoContentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Video URL saved", draftService.updateLessonVideoUrl(draftId, sectionId, lessonId, request)));
    }

    @PutMapping("/{draftId}/sections/{sectionId}/lessons/{lessonId}/text-url")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<DraftResponse>> updateTextUrl(
            @PathVariable String draftId,
            @PathVariable String sectionId,
            @PathVariable String lessonId,
            @Valid @RequestBody UpdateTextContentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Text URL saved", draftService.updateLessonTextUrl(draftId, sectionId, lessonId, request)));
    }
}
