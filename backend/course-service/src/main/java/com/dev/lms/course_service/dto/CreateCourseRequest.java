package com.dev.lms.course_service.dto;

import com.dev.lms.course_service.entity.CourseLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateCourseRequest(
        @NotNull UUID instructorId,
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
