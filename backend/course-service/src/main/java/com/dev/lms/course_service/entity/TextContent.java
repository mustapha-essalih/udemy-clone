package com.dev.lms.course_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "text_content")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TextContent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "text_content_id")
    private UUID textContentId;

    @Column(name = "lesson_id", unique = true, nullable = false)
    private UUID lessonId;

    @Column(name = "text_url", nullable = false, columnDefinition = "TEXT")
    private String textUrl;
}
