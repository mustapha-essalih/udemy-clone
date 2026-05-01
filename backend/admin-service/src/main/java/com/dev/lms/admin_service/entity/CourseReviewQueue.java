package com.dev.lms.admin_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "course_reviews_queue")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CourseReviewQueue {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "course_id")
    private UUID courseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReviewQueueStatus status = ReviewQueueStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}
