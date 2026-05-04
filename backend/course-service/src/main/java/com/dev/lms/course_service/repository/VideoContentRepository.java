package com.dev.lms.course_service.repository;

import com.dev.lms.course_service.entity.VideoContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VideoContentRepository extends JpaRepository<VideoContent, UUID> {
    Optional<VideoContent> findByLessonId(UUID lessonId);
}
