package com.dev.lms.content_service.controller;

import com.dev.lms.content_service.service.StreamingService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/content")
@RequiredArgsConstructor
public class StreamController {

    private final StreamingService streamingService;

    @GetMapping("/{mediaId}/stream")
    public Mono<ResponseEntity<Flux<DataBuffer>>> stream(
            @PathVariable UUID mediaId,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) {
        return streamingService.stream(mediaId, rangeHeader);
    }
}
