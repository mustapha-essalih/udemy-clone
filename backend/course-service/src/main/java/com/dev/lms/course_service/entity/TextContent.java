package com.dev.lms.course_service.entity;

import java.util.UUID;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "text_content")
@Getter 
@Setter 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
public class TextContent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "text_content_id")
    private UUID textContentId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false, unique = true)
    private Lesson lesson;

    @Column(name = "text_url", nullable = false, columnDefinition = "TEXT")
    private String textUrl;
}
