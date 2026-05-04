package com.dev.lms.admin_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateSubCategoryRequest(
        @NotBlank(message = "Subcategory name is required")
        @Size(max = 100, message = "Subcategory name must be at most 100 characters")
        String name,

        @NotNull(message = "Category ID is required")
        UUID categoryId
) {}
