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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses/subcategories")
@RequiredArgsConstructor
public class SubCategoryController {

    private final SubCategoryService subCategoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SubCategoryDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(subCategoryService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SubCategoryDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(subCategoryService.getById(id)));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<List<SubCategoryDto>>> getByCategoryId(@PathVariable UUID categoryId) {
        return ResponseEntity.ok(ApiResponse.ok(subCategoryService.getByCategoryId(categoryId)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SubCategoryDto>> create(@Valid @RequestBody CreateSubCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Subcategory created", subCategoryService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SubCategoryDto>> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateSubCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Subcategory updated", subCategoryService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        subCategoryService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Subcategory deleted"));
    }
}
