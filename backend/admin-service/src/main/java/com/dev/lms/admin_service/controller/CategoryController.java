package com.dev.lms.admin_service.controller;

import com.dev.lms.admin_service.client.CourseServiceClient;
import com.dev.lms.admin_service.dto.CategoryDto;
import com.dev.lms.admin_service.dto.CreateCategoryRequest;
import com.dev.lms.admin_service.dto.UpdateCategoryRequest;
import com.dev.lms.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CourseServiceClient courseServiceClient;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok("Categories fetched", courseServiceClient.getAllCategories()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(courseServiceClient.getCategoryById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryDto>> create(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryDto created = courseServiceClient.createCategory(request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Category created", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryDto>> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Category updated", courseServiceClient.updateCategory(id, request.name())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        courseServiceClient.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.ok("Category deleted"));
    }
}
