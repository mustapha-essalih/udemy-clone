package com.dev.lms.course_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "video_content")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class VideoContent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "video_content_id")
    private UUID videoContentId;

    @Column(name = "lesson_id", unique = true, nullable = false)
    private UUID lessonId;

    @Column(name = "video_url", nullable = false, columnDefinition = "TEXT")
    private String videoUrl;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "is_preview")
    @Builder.Default
    private Boolean isPreview = false;
}
