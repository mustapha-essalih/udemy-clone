package com.dev.lms.course_service.controller;

import com.dev.lms.common.response.ApiResponse;
import com.dev.lms.course_service.dto.CourseOverviewResponse;
import com.dev.lms.course_service.dto.DraftResponse;
import com.dev.lms.course_service.dto.RejectDraftRequest;
import com.dev.lms.course_service.service.CourseDraftService;
import com.dev.lms.course_service.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses/manager")
@RequiredArgsConstructor
public class ManagerCourseController {

    private final CourseDraftService draftService;
    private final CourseService courseService;

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<List<CourseOverviewResponse>>> listAllCourses(
            @RequestHeader("X-User-Role") String role,
            @RequestHeader("X-User-Id") String userId) {
        UUID instructorId = UUID.fromString(userId);
        List<CourseOverviewResponse> all = new ArrayList<>();
        all.addAll(draftService.listAllDrafts(role, instructorId));
        all.addAll(courseService.listAllAsOverview(role, instructorId));
        return ResponseEntity.ok(ApiResponse.ok(all));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<DraftResponse>>> listPendingCourses() {
        return ResponseEntity.ok(ApiResponse.ok(draftService.listPending()));
    }

    @PostMapping("/{draftId}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Object>> approveCourse(@PathVariable String draftId) {
        return ResponseEntity.ok(ApiResponse.ok("Course approved and published",  draftService.approve(draftId)));
    }

    @PostMapping("/{draftId}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DraftResponse>> rejectCourse(
            @PathVariable String draftId,
            @Valid @RequestBody RejectDraftRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Course rejected", draftService.reject(draftId, request.feedback())));
    }
}