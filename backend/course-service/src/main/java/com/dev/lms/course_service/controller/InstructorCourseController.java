package com.dev.lms.course_service.controller;

import com.dev.lms.common.response.ApiResponse;
import com.dev.lms.course_service.dto.CreateDraftRequest;
import com.dev.lms.course_service.dto.DraftResponse;
import com.dev.lms.course_service.dto.UpdateDraftRequest;
import com.dev.lms.course_service.dto.UpdateTextContentRequest;
import com.dev.lms.course_service.dto.UpdateVideoContentRequest;
import com.dev.lms.course_service.service.CourseDraftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses/instructor")
@RequiredArgsConstructor
public class InstructorCourseController {

    private final CourseDraftService draftService;

    @GetMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<List<DraftResponse>>> listMyCourses(
            @RequestHeader("X-User-Id") String userId) {
                System.out.println(userId);
        return ResponseEntity.ok(ApiResponse.ok(draftService.listByInstructor(UUID.fromString(userId))));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<DraftResponse>> createCourse(
            @Valid @RequestBody CreateDraftRequest request,
            @RequestHeader("X-User-Id") String userId) {
        DraftResponse draft = draftService.create(UUID.fromString(userId), request);
        return ResponseEntity.ok(ApiResponse.ok("Course draft created", draft));
    }

    @GetMapping("/{draftId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DraftResponse>> getCourseById(
            @PathVariable String draftId,
            @RequestHeader("X-Role") String role,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.ok(draftService.getById(draftId, role, UUID.fromString(userId))));
    }

    @PutMapping("/{draftId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<DraftResponse>> updateCourse(
            @PathVariable String draftId,
            @Valid @RequestBody UpdateDraftRequest request,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.ok("Draft updated", draftService.update(draftId, UUID.fromString(userId), request)));
    }

    @PostMapping("/{draftId}/submit")
    @PreAuthorize("hasAnyRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<DraftResponse>> submitForReview(
            @PathVariable String draftId,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.ok("Course submitted for review", draftService.submit(draftId, UUID.fromString(userId))));
    }

    @PutMapping("/{draftId}/sections/{sectionId}/lessons/{lessonId}/video-url")
    @PreAuthorize("hasAnyRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<DraftResponse>> updateVideoUrl(
            @PathVariable String draftId,
            @PathVariable String sectionId,
            @PathVariable String lessonId,
            @Valid @RequestBody UpdateVideoContentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Video URL saved", draftService.updateLessonVideoUrl(draftId, sectionId, lessonId, request)));
    }

    @PutMapping("/{draftId}/sections/{sectionId}/lessons/{lessonId}/text-url")
    @PreAuthorize("hasAnyRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<DraftResponse>> updateTextUrl(
            @PathVariable String draftId,
            @PathVariable String sectionId,
            @PathVariable String lessonId,
            @Valid @RequestBody UpdateTextContentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Text URL saved", draftService.updateLessonTextUrl(draftId, sectionId, lessonId, request)));
    }

    @DeleteMapping("/{draftId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(
            @PathVariable String draftId,
            @RequestHeader("X-User-Id") String userId) {
        draftService.delete(draftId, UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.ok("Draft deleted"));
    }
}