package com.dev.lms.content_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "media_files")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MediaFile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "file_name")
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", length = 20)
    private FileType fileType;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    private Long size;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_provider", length = 10)
    private StorageProvider storageProvider;

    @Column(name = "storage_key", columnDefinition = "TEXT")
    private String storageKey;

    @Column(columnDefinition = "TEXT")
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MediaStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
