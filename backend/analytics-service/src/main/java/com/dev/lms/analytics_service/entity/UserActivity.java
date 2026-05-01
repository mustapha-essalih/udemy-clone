package com.dev.lms.analytics_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_activity")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class UserActivity {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "total_courses_enrolled")
    private Integer totalCoursesEnrolled;
}
