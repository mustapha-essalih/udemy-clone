package com.dev.lms.course_service.dto;

import com.dev.lms.course_service.draft.SectionDraft;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record DraftResponse(
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
        List<SectionDraft> sections,
        String status,
        String rejectionFeedback,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt
) {}
