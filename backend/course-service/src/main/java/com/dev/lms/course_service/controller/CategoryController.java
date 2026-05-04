package com.dev.lms.course_service.controller;

import com.dev.lms.common.response.ApiResponse;
import com.dev.lms.course_service.dto.CategoryDto;
import com.dev.lms.course_service.dto.CreateCategoryRequest;
import com.dev.lms.course_service.dto.UpdateCategoryRequest;
import com.dev.lms.course_service.service.CategoryService;
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
@RequestMapping("/api/courses/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<CategoryDto>>>> getAll() {
        return Mono.fromCallable(() -> categoryService.getAllCategories())
                .subscribeOn(Schedulers.boundedElastic())
                .map(categories -> ResponseEntity.ok(ApiResponse.ok("Categories fetched", categories)));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<CategoryDto>>> getById(@PathVariable UUID id) {
        return Mono.fromCallable(() -> categoryService.getById(id))
                .subscribeOn(Schedulers.boundedElastic())
                .map(response -> ResponseEntity.ok(ApiResponse.ok(response)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<ApiResponse<CategoryDto>>> create(@Valid @RequestBody CreateCategoryRequest request) {
        return Mono.fromCallable(() -> categoryService.create(request))
                .subscribeOn(Schedulers.boundedElastic())
                .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.ok("Category created", response)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<ApiResponse<CategoryDto>>> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateCategoryRequest request) {
        return Mono.fromCallable(() -> categoryService.update(id, request))
                .subscribeOn(Schedulers.boundedElastic())
                .map(response -> ResponseEntity.ok(ApiResponse.ok("Category updated", response)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<ApiResponse<Void>>> delete(@PathVariable UUID id) {
        return Mono.fromRunnable(() -> categoryService.delete(id))
                .subscribeOn(Schedulers.boundedElastic())
                .then(Mono.just(ResponseEntity.ok(ApiResponse.ok("Category deleted"))));
    }
}
