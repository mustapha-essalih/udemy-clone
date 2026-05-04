package com.dev.lms.course_service.controller;

import com.dev.lms.common.response.ApiResponse;
import com.dev.lms.course_service.dto.CourseResponse;
import com.dev.lms.course_service.dto.CreateCourseRequest;
import com.dev.lms.course_service.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<CourseResponse>>> create(
            @Valid @RequestBody CreateCourseRequest request) {
        return courseService.create(request)
                .map(course -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.ok("Course created", course)));
    }

    @GetMapping("/{courseId}")
    public Mono<ResponseEntity<ApiResponse<CourseResponse>>> getById(@PathVariable UUID courseId) {
        return courseService.getById(courseId)
                .map(course -> ResponseEntity.ok(ApiResponse.ok(course)));
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<CourseResponse>>>> listByInstructor(
            @RequestParam UUID instructorId) {
        return courseService.listByInstructor(instructorId)
                .map(courses -> ResponseEntity.ok(ApiResponse.ok(courses)));
    }
}
