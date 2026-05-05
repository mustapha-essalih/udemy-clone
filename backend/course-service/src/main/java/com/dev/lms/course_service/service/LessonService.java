package com.dev.lms.course_service.service;

import com.dev.lms.course_service.dto.LessonResponse;
import com.dev.lms.course_service.dto.UpdateTextContentRequest;
import com.dev.lms.course_service.dto.UpdateVideoContentRequest;
import com.dev.lms.course_service.entity.*;
import com.dev.lms.course_service.exception.BusinessException;
import com.dev.lms.course_service.exception.ResourceNotFoundException;
import com.dev.lms.course_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;
    private final SectionRepository sectionRepository;
    private final VideoContentRepository videoContentRepository;
    private final TransactionTemplate transactionTemplate;

    public LessonResponse updateVideoContent(UUID courseId, UUID sectionId, UUID lessonId,
                                              UpdateVideoContentRequest req) {
        return transactionTemplate.execute(status -> {
            Section section = sectionRepository.findById(sectionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Section", sectionId));
            if (!section.getCourseId().equals(courseId)) {
                throw new BusinessException("Section does not belong to course " + courseId);
            }

            Lesson lesson = lessonRepository.findById(lessonId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lesson", lessonId));
            if (!lesson.getSectionId().equals(sectionId)) {
                throw new BusinessException("Lesson does not belong to section " + sectionId);
            }
            if (lesson.getLessonType() != LessonType.VIDEO) {
                throw new BusinessException("Lesson is not of type VIDEO");
            }

            VideoContent vc = videoContentRepository.findByLessonId(lessonId)
                    .orElseGet(() -> VideoContent.builder().lessonId(lessonId).build());
            vc.setVideoUrl(req.videoUrl());
            vc.setDurationMinutes(req.durationMinutes());
            vc.setIsPreview(req.isPreview() != null ? req.isPreview() : false);

            VideoContent saved = videoContentRepository.save(vc);

            return new LessonResponse(
                    lesson.getLessonId(), lesson.getSectionId(), lesson.getTitle(),
                    lesson.getLessonType().name(), saved.getVideoUrl(), null,
                    saved.getDurationMinutes(), saved.getIsPreview(), lesson.getCreatedAt()
            );
        });
    }

    public LessonResponse updateTextContent(UUID courseId, UUID sectionId, UUID lessonId,
                                             UpdateTextContentRequest req) {
        return transactionTemplate.execute(status -> {
            Section section = sectionRepository.findById(sectionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Section", sectionId));
            if (!section.getCourseId().equals(courseId)) {
                throw new BusinessException("Section does not belong to course " + courseId);
            }

            Lesson lesson = lessonRepository.findById(lessonId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lesson", lessonId));
            if (!lesson.getSectionId().equals(sectionId)) {
                throw new BusinessException("Lesson does not belong to section " + sectionId);
            }
            if (lesson.getLessonType() == LessonType.VIDEO) {
                throw new BusinessException("Lesson is of type VIDEO, use the /video-url endpoint");
            }

            lesson.setTextUrl(req.contentUrl());
            Lesson saved = lessonRepository.save(lesson);

            return new LessonResponse(
                    saved.getLessonId(), saved.getSectionId(), saved.getTitle(),
                    saved.getLessonType().name(), null, saved.getTextUrl(),
                    null, null, saved.getCreatedAt()
            );
        });
    }
}
