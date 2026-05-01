package com.dev.lms.course_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "lesson_resources")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonResource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "lesson_id")
    private UUID lessonId;

    @Column(name = "media_id")
    private UUID mediaId;

    private String title;

    @Enumerated(EnumType.STRING)
    private ResourceType type;
}
