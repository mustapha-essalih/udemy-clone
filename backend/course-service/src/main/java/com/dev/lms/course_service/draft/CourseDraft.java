package com.dev.lms.course_service.draft;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDraft {
    private String draftId;
    private UUID instructorId;
    private UUID publishedCourseId;
    private String title;
    private String subTitle;
    private String description;
    private BigDecimal price;
    private Boolean isFree;
    private String language;
    private String couponCode;
    private Integer courseDurationMinutes;
    private String level;
    private List<SectionDraft> sections;
    private DraftStatus status;
    private String rejectionFeedback;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
}
