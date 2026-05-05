package com.dev.lms.admin_service.repository;

import com.dev.lms.admin_service.entity.CourseReviewQueue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CourseReviewQueueRepository extends JpaRepository<CourseReviewQueue, UUID> {
}
