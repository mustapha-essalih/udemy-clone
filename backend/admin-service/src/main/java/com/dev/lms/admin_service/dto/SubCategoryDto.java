package com.dev.lms.admin_service.dto;

import java.util.UUID;

public record SubCategoryDto(
        UUID id,
        String name,
        UUID categoryId,
        String categoryName
) {}
