package com.dev.lms.course_service.controller;

import com.dev.lms.common.response.ApiResponse;
import com.dev.lms.course_service.dto.CourseResponse;
import com.dev.lms.course_service.service.CourseService;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/{courseId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CourseResponse>> getCourse(@PathVariable UUID courseId) {
        return ResponseEntity.ok(ApiResponse.ok(courseService.getById(courseId)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> listPublishedCourses(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.ok(courseService.listByInstructor(UUID.fromString(userId))));
    }
}
