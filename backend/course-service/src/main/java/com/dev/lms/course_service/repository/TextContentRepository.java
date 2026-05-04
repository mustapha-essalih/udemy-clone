package com.dev.lms.course_service.repository;

import com.dev.lms.course_service.entity.TextContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TextContentRepository extends JpaRepository<TextContent, UUID> {
    Optional<TextContent> findByLessonId(UUID lessonId);
}
