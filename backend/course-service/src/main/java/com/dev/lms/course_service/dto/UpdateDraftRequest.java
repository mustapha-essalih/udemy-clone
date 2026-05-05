package com.dev.lms.course_service.dto;

import com.dev.lms.course_service.entity.CourseLevel;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.List;

public record UpdateDraftRequest(
        String title,
        String subTitle,
        String description,
        BigDecimal price,
        Boolean isFree,
        String language,
        String couponCode,
        Integer courseDurationMinutes,
        CourseLevel level,
        @Valid List<CreateSectionRequest> sections
) {}
