package com.dev.lms.admin_service.service;

import com.dev.lms.admin_service.client.CourseServiceClient;
import com.dev.lms.admin_service.dto.CourseDraftDto;
import com.dev.lms.admin_service.entity.CourseReviewQueue;
import com.dev.lms.admin_service.entity.ReviewQueueStatus;
import com.dev.lms.admin_service.repository.CourseReviewQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseReviewService {

    private final CourseServiceClient courseServiceClient;
    private final CourseReviewQueueRepository reviewQueueRepository;

    public List<CourseDraftDto> getPendingCourses() {
        return courseServiceClient.getPendingDrafts();
    }

    public CourseDraftDto getDraft(String draftId) {
        return courseServiceClient.getDraft(draftId);
    }

    @Transactional
    public Object approve(String draftId, UUID reviewerId) {
        Object course = courseServiceClient.approveDraft(draftId);

        reviewQueueRepository.save(CourseReviewQueue.builder()
                .courseId(UUID.fromString(draftId))
                .status(ReviewQueueStatus.APPROVED)
                .reviewedBy(reviewerId)
                .reviewedAt(LocalDateTime.now())
                .build());

        log.info("Course draft {} approved by reviewer {}", draftId, reviewerId);
        return course;
    }

    @Transactional
    public CourseDraftDto reject(String draftId, String feedback, UUID reviewerId) {
        CourseDraftDto draft = courseServiceClient.rejectDraft(draftId, feedback);

        reviewQueueRepository.save(CourseReviewQueue.builder()
                .courseId(UUID.fromString(draftId))
                .status(ReviewQueueStatus.REJECTED)
                .feedback(feedback)
                .reviewedBy(reviewerId)
                .reviewedAt(LocalDateTime.now())
                .build());

        log.info("Course draft {} rejected by reviewer {}", draftId, reviewerId);
        return draft;
    }
}
