package com.dev.lms.course_service.dto;

import java.util.List;
import java.util.UUID;

public record CategoryDto(
        UUID id,
        String name,
        List<SubCategoryDto> subCategories
) {}
