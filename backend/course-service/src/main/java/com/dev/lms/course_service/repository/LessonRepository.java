package com.dev.lms.course_service.repository;

import com.dev.lms.course_service.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {
    List<Lesson> findBySection_SectionId(UUID sectionId);
}
