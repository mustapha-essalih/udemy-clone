package com.dev.lms.content_service.repository;

import com.dev.lms.content_service.entity.UploadSession;
import com.dev.lms.content_service.entity.UploadStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface UploadSessionRepository extends JpaRepository<UploadSession, UUID> {

    @Modifying
    @Transactional
    @Query("""
            UPDATE UploadSession s
            SET s.uploadedChunks = s.uploadedChunks + 1,
                s.uploadedBytes  = s.uploadedBytes  + :chunkBytes,
                s.status         = :status
            WHERE s.id = :sessionId
            """)
    void incrementProgress(
            @Param("sessionId") UUID sessionId,
            @Param("chunkBytes") long chunkBytes,
            @Param("status") UploadStatus status
    );
}
