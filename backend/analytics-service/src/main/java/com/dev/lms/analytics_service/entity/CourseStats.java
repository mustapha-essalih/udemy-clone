package com.dev.lms.analytics_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "course_stats")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CourseStats {
    @Id
    @Column(name = "course_id")
    private UUID courseId;

    @Column(name = "total_enrollments")
    private Integer totalEnrollments;

    @Column(name = "total_revenue")
    private BigDecimal totalRevenue;

    @Column(name = "avg_rating")
    private BigDecimal avgRating;
}
