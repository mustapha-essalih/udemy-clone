package com.dev.lms.course_service.controller;

import com.dev.lms.common.response.ApiResponse;
import com.dev.lms.course_service.dto.LessonResponse;
import com.dev.lms.course_service.dto.UpdateTextContentRequest;
import com.dev.lms.course_service.dto.UpdateVideoContentRequest;
import com.dev.lms.course_service.service.LessonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/courses/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    @PutMapping("/{courseId}/sections/{sectionId}/lessons/{lessonId}/video-url")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<LessonResponse>> updateVideoContent(
            @PathVariable UUID courseId,
            @PathVariable UUID sectionId,
            @PathVariable UUID lessonId,
            @Valid @RequestBody UpdateVideoContentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Video content updated",
                lessonService.updateVideoContent(courseId, sectionId, lessonId, request)));
    }

    @PutMapping("/{courseId}/sections/{sectionId}/lessons/{lessonId}/text-url")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<LessonResponse>> updateTextContent(
            @PathVariable UUID courseId,
            @PathVariable UUID sectionId,
            @PathVariable UUID lessonId,
            @Valid @RequestBody UpdateTextContentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Text content updated",
                lessonService.updateTextContent(courseId, sectionId, lessonId, request)));
    }
}
// ourse details page