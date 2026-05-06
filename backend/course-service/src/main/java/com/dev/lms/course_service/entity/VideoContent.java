package com.dev.lms.course_service.entity;

import java.util.UUID;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "video_content")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class VideoContent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "video_content_id")
    private UUID videoContentId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false, unique = true)
    private Lesson lesson;

    @Column(name = "video_url", nullable = false, columnDefinition = "TEXT")
    private String videoUrl;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "is_preview")
    @Builder.Default
    private Boolean isPreview = false;
}
