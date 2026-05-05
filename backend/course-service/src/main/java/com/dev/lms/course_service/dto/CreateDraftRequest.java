package com.dev.lms.course_service.dto;

import com.dev.lms.course_service.entity.CourseLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreateDraftRequest(
        @NotBlank String title,
        @NotBlank String subTitle,
        @NotBlank String description,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        Boolean isFree,
        @NotBlank String language,
        String couponCode,
        @NotNull @Min(1) Integer courseDurationMinutes,
        CourseLevel level,
        @NotEmpty @Valid List<CreateSectionRequest> sections
) {}
