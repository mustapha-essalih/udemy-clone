package com.dev.lms.course_service.repository;

import com.dev.lms.course_service.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SectionRepository extends JpaRepository<Section, UUID> {
    List<Section> findByCourse_CourseId(UUID courseId);
}
