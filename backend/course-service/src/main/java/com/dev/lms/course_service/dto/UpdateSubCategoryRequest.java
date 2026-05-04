package com.dev.lms.course_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSubCategoryRequest(
        @NotBlank(message = "Subcategory name is required")
        @Size(max = 100, message = "Subcategory name must be at most 100 characters")
        String name
) {}
