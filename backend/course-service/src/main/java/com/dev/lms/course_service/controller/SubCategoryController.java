package com.dev.lms.course_service.controller;

import com.dev.lms.common.response.ApiResponse;
import com.dev.lms.course_service.dto.CreateSubCategoryRequest;
import com.dev.lms.course_service.dto.SubCategoryDto;
import com.dev.lms.course_service.dto.UpdateSubCategoryRequest;
import com.dev.lms.course_service.service.SubCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses/subcategories")
@RequiredArgsConstructor
public class SubCategoryController {

    private final SubCategoryService subCategoryService;

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<SubCategoryDto>>>> getAll() {
        return Mono.fromCallable(() -> subCategoryService.getAll())
                .subscribeOn(Schedulers.boundedElastic())
                .map(responses -> ResponseEntity.ok(ApiResponse.ok(responses)));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<SubCategoryDto>>> getById(@PathVariable UUID id) {
        return Mono.fromCallable(() -> subCategoryService.getById(id))
                .subscribeOn(Schedulers.boundedElastic())
                .map(response -> ResponseEntity.ok(ApiResponse.ok(response)));
    }

    @GetMapping("/category/{categoryId}")
    public Mono<ResponseEntity<ApiResponse<List<SubCategoryDto>>>> getByCategoryId(@PathVariable UUID categoryId) {
        return Mono.fromCallable(() -> subCategoryService.getByCategoryId(categoryId))
                .subscribeOn(Schedulers.boundedElastic())
                .map(responses -> ResponseEntity.ok(ApiResponse.ok(responses)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<ApiResponse<SubCategoryDto>>> create(@Valid @RequestBody CreateSubCategoryRequest request) {
        return Mono.fromCallable(() -> subCategoryService.create(request))
                .subscribeOn(Schedulers.boundedElastic())
                .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.ok("Subcategory created", response)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<ApiResponse<SubCategoryDto>>> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateSubCategoryRequest request) {
        return Mono.fromCallable(() -> subCategoryService.update(id, request))
                .subscribeOn(Schedulers.boundedElastic())
                .map(response -> ResponseEntity.ok(ApiResponse.ok("Subcategory updated", response)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<ApiResponse<Void>>> delete(@PathVariable UUID id) {
        return Mono.fromRunnable(() -> subCategoryService.delete(id))
                .subscribeOn(Schedulers.boundedElastic())
                .then(Mono.just(ResponseEntity.ok(ApiResponse.ok("Subcategory deleted"))));
    }
}
