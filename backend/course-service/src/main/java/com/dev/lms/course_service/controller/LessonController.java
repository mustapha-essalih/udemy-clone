package com.dev.lms.course_service.controller;

import com.dev.lms.common.response.ApiResponse;
import com.dev.lms.course_service.dto.LessonResponse;
import com.dev.lms.course_service.dto.UpdateVideoContentRequest;
import com.dev.lms.course_service.service.LessonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    @PutMapping("/{courseId}/sections/{sectionId}/lessons/{lessonId}/video-url")
    public Mono<ResponseEntity<ApiResponse<LessonResponse>>> updateVideoContent(
            @PathVariable UUID courseId,
            @PathVariable UUID sectionId,
            @PathVariable UUID lessonId,
            @Valid @RequestBody UpdateVideoContentRequest request) {
        return lessonService.updateVideoContent(courseId, sectionId, lessonId, request)
                .map(lesson -> ResponseEntity.ok(ApiResponse.ok("Video content updated", lesson)));
    }
}
