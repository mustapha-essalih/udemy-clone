package com.dev.lms.course_service.service;

import com.dev.lms.course_service.dto.*;
import com.dev.lms.course_service.entity.*;
import com.dev.lms.course_service.exception.ResourceNotFoundException;
import com.dev.lms.course_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final SectionRepository sectionRepository;
    private final LessonRepository lessonRepository;
    private final VideoContentRepository videoContentRepository;
    private final TransactionTemplate transactionTemplate;

    public Mono<CourseResponse> create(CreateCourseRequest req) {
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            Course course = Course.builder()
                    .instructorId(req.instructorId())
                    .title(req.title())
                    .subTitle(req.subTitle())
                    .description(req.description())
                    .price(req.price())
                    .isFree(req.isFree() != null ? req.isFree() : false)
                    .language(req.language())
                    .couponCode(req.couponCode())
                    .courseDurationMinutes(req.courseDurationMinutes())
                    .level(req.level() != null ? req.level() : CourseLevel.ALL_LEVELS)
                    .build();

            Course savedCourse = courseRepository.save(course);

            List<SectionResponse> sectionResponses = new ArrayList<>();
            for (CreateSectionRequest sectionReq : req.sections()) {
                Section section = Section.builder()
                        .courseId(savedCourse.getCourseId())
                        .title(sectionReq.title())
                        .build();
                Section savedSection = sectionRepository.save(section);

                List<LessonResponse> lessonResponses = new ArrayList<>();
                for (CreateLessonRequest lessonReq : sectionReq.lessons()) {
                    Lesson lesson = Lesson.builder()
                            .sectionId(savedSection.getSectionId())
                            .title(lessonReq.title())
                            .lessonType(lessonReq.lessonType())
                            .build();
                    Lesson savedLesson = lessonRepository.save(lesson);
                    lessonResponses.add(toLessonResponse(savedLesson, null));
                }
                sectionResponses.add(toSectionResponse(savedSection, lessonResponses));
            }

            return toCourseResponse(savedCourse, sectionResponses);
        })).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<CourseResponse> getById(UUID courseId) {
        return Mono.fromCallable(() -> {
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

            List<SectionResponse> sections = sectionRepository.findByCourseId(courseId).stream()
                    .map(section -> {
                        List<LessonResponse> lessons = lessonRepository
                                .findBySectionId(section.getSectionId()).stream()
                                .map(lesson -> {
                                    VideoContent vc = videoContentRepository
                                            .findByLessonId(lesson.getLessonId()).orElse(null);
                                    return toLessonResponse(lesson, vc);
                                })
                                .toList();
                        return toSectionResponse(section, lessons);
                    })
                    .toList();

            return toCourseResponse(course, sections);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<List<CourseResponse>> listByInstructor(UUID instructorId) {
        return Mono.fromCallable(() ->
                courseRepository.findByInstructorId(instructorId).stream()
                        .map(course -> toCourseResponse(course, List.of()))
                        .toList()
        ).subscribeOn(Schedulers.boundedElastic());
    }

    private CourseResponse toCourseResponse(Course c, List<SectionResponse> sections) {
        return new CourseResponse(
                c.getCourseId(), c.getInstructorId(), c.getTitle(), c.getSubTitle(),
                c.getDescription(), c.getPrice(), c.getIsFree(), c.getLanguage(),
                c.getCouponCode(), c.getRating(), c.getCourseDurationMinutes(),
                c.getStatus().name(), c.getLevel().name(), sections, c.getCreatedAt()
        );
    }

    private SectionResponse toSectionResponse(Section s, List<LessonResponse> lessons) {
        return new SectionResponse(s.getSectionId(), s.getCourseId(), s.getTitle(), lessons);
    }

    private LessonResponse toLessonResponse(Lesson l, VideoContent vc) {
        return new LessonResponse(
                l.getLessonId(), l.getSectionId(), l.getTitle(), l.getLessonType().name(),
                vc != null ? vc.getVideoUrl() : null,
                l.getTextUrl(),
                vc != null ? vc.getDurationMinutes() : null,
                vc != null ? vc.getIsPreview() : null,
                l.getCreatedAt()
        );
    }
}
