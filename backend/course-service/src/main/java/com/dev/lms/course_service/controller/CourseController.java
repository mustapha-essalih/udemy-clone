package com.dev.lms.course_service.controller;

import com.dev.lms.common.response.ApiResponse;
import com.dev.lms.course_service.dto.CourseResponse;
import com.dev.lms.course_service.dto.CreateDraftRequest;
import com.dev.lms.course_service.dto.DraftResponse;
import com.dev.lms.course_service.service.CourseDraftService;
import com.dev.lms.course_service.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final CourseDraftService courseDraftService;

    @PostMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<DraftResponse>> create(
            @Valid @RequestBody CreateDraftRequest request,
            @RequestHeader("X-User-Id") String userId) {
        DraftResponse draft = courseDraftService.create(UUID.fromString(userId), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Course draft created", draft));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> listByInstructor(
            @RequestParam UUID instructorId) {
        return ResponseEntity.ok(ApiResponse.ok(courseService.listByInstructor(instructorId)));
    }
}
