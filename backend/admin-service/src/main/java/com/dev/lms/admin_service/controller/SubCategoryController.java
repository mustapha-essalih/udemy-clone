package com.dev.lms.admin_service.controller;

import com.dev.lms.admin_service.client.CourseServiceClient;
import com.dev.lms.admin_service.dto.CreateSubCategoryRequest;
import com.dev.lms.admin_service.dto.SubCategoryDto;
import com.dev.lms.admin_service.dto.UpdateSubCategoryRequest;
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
@RequestMapping("/api/admin/subcategories")
@RequiredArgsConstructor
public class SubCategoryController {

    private final CourseServiceClient courseServiceClient;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SubCategoryDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(courseServiceClient.getAllSubCategories()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SubCategoryDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(courseServiceClient.getSubCategoryById(id)));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<List<SubCategoryDto>>> getByCategoryId(@PathVariable UUID categoryId) {
        return ResponseEntity.ok(ApiResponse.ok(courseServiceClient.getSubCategoriesByCategoryId(categoryId)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SubCategoryDto>> create(@Valid @RequestBody CreateSubCategoryRequest request) {
        SubCategoryDto created = courseServiceClient.createSubCategory(request.name(), request.categoryId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Subcategory created", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SubCategoryDto>> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateSubCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Subcategory updated", courseServiceClient.updateSubCategory(id, request.name())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        courseServiceClient.deleteSubCategory(id);
        return ResponseEntity.ok(ApiResponse.ok("Subcategory deleted"));
    }
}
