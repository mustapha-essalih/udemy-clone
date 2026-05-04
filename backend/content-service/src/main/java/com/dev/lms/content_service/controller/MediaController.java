package com.dev.lms.content_service.controller;

import com.dev.lms.common.response.ApiResponse;
import com.dev.lms.content_service.dto.MediaResponse;
import com.dev.lms.content_service.entity.MediaFile;
import com.dev.lms.content_service.exception.ResourceNotFoundException;
import com.dev.lms.content_service.repository.MediaFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

@RestController
@RequestMapping("/api/content")
@RequiredArgsConstructor
public class MediaController {

    private final MediaFileRepository mediaFileRepository;

    @GetMapping("/{mediaId}")
    public Mono<ApiResponse<MediaResponse>> getMedia(@PathVariable UUID mediaId) {
        return Mono.fromCallable(() -> {
            MediaFile file = mediaFileRepository.findById(mediaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Media", mediaId));
            return ApiResponse.ok(toResponse(file));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private MediaResponse toResponse(MediaFile f) {
        return new MediaResponse(
                f.getId().toString(),
                f.getCourseId(), f.getSectionId(), f.getLessonId(),
                f.getUrl(), f.getFileName(),
                f.getFileType() != null ? f.getFileType().name() : null,
                f.getMimeType(), f.getSize(),
                f.getStatus().name()
        );
    }
}
