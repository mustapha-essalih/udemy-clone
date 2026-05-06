package com.dev.lms.course_service.entity;

import java.util.UUID;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "resources")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "resource_id")
    private UUID resourceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(nullable = false)
    private String title;

    @Column(name = "resource_url", nullable = false, columnDefinition = "TEXT")
    private String resourceUrl;

    @Column(name = "is_preview")
    @Builder.Default
    private Boolean isPreview = false;

    @Column(name = "file_type", length = 50)
    private String fileType;
}
