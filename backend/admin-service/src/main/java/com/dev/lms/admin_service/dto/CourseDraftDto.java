package com.dev.lms.admin_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CourseDraftDto(
        String draftId,
        UUID instructorId,
        String title,
        String subTitle,
        String description,
        BigDecimal price,
        Boolean isFree,
        String language,
        String couponCode,
        Integer courseDurationMinutes,
        String level,
        List<Object> sections,
        String status,
        String rejectionFeedback,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt
) {}
