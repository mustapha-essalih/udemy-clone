package com.dev.lms.course_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "lessons")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "lesson_id")
    private UUID lessonId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "lesson_type", nullable = false, length = 10)
    private LessonType lessonType;

    @Column(name = "text_url", columnDefinition = "TEXT")
    private String textUrl;

    @OneToOne(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    private VideoContent videoContent;

    @OneToOne(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    private TextContent textContent;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Resource> resources = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
