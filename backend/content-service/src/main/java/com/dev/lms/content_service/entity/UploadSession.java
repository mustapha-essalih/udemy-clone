package com.dev.lms.content_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "upload_sessions")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class UploadSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "section_id", nullable = false)
    private UUID sectionId;

    @Column(name = "lesson_id", nullable = false)
    private UUID lessonId;

    @Column(name = "course_name")
    private String courseName;

    @Column(name = "section_name")
    private String sectionName;

    @Column(name = "lesson_title")
    private String lessonTitle;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "total_size", nullable = false)
    private Long totalSize;

    @Column(name = "chunk_size", nullable = false)
    private Long chunkSize;

    @Column(name = "total_chunks", nullable = false)
    private Integer totalChunks;

    @Column(name = "uploaded_chunks", nullable = false)
    @Builder.Default
    private Integer uploadedChunks = 0;

    @Column(name = "uploaded_bytes", nullable = false)
    @Builder.Default
    private Long uploadedBytes = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UploadStatus status = UploadStatus.PENDING;

    @Column(name = "temp_path", columnDefinition = "TEXT")
    private String tempPath;

    @Column(name = "media_id")
    private UUID mediaId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
