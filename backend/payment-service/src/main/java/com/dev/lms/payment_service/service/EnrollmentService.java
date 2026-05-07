package com.dev.lms.payment_service.service;

import com.dev.lms.payment_service.dto.EnrollmentResponse;
import com.dev.lms.payment_service.dto.LessonProgressRequest;
import com.dev.lms.payment_service.entity.Enrollment;
import com.dev.lms.payment_service.entity.LessonProgress;
import com.dev.lms.payment_service.exception.BusinessException;
import com.dev.lms.payment_service.exception.ResourceNotFoundException;
import com.dev.lms.payment_service.repository.EnrollmentRepository;
import com.dev.lms.payment_service.repository.LessonProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;

    public List<EnrollmentResponse> getEnrollmentsByUser(UUID userId) {
        return enrollmentRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public boolean isEnrolled(UUID userId, UUID courseId) {
        return enrollmentRepository.existsByUserIdAndCourseId(userId, courseId);
    }

    public List<LessonProgress> getLessonProgress(UUID enrollmentId, UUID requestingUserId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", enrollmentId));

        if (!enrollment.getUserId().equals(requestingUserId)) {
            throw new BusinessException("Access denied to this enrollment");
        }

        return lessonProgressRepository.findByEnrollmentId(enrollmentId);
    }

    @Transactional
    public LessonProgress upsertLessonProgress(UUID enrollmentId, LessonProgressRequest request, UUID requestingUserId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", enrollmentId));

        if (!enrollment.getUserId().equals(requestingUserId)) {
            throw new BusinessException("Access denied to this enrollment");
        }

        LessonProgress progress = lessonProgressRepository
                .findByEnrollmentIdAndLessonId(enrollmentId, request.lessonId())
                .orElse(LessonProgress.builder()
                        .enrollmentId(enrollmentId)
                        .lessonId(request.lessonId())
                        .build());

        progress.setCompleted(request.completed());
        progress.setProgressPercent(request.progressPercent());
        progress.setLastPositionSeconds(request.lastPositionSeconds());

        return lessonProgressRepository.save(progress);
    }

    private EnrollmentResponse toResponse(Enrollment e) {
        return new EnrollmentResponse(e.getId(), e.getUserId(), e.getCourseId(),
                e.getPaymentId(), e.getStatus(), e.getEnrolledAt());
    }
}
