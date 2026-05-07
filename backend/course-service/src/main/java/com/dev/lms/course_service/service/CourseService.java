package com.dev.lms.course_service.service;

import com.dev.lms.course_service.dto.*;
import com.dev.lms.course_service.entity.*;
import com.dev.lms.course_service.event.CourseIndexEvent;
import com.dev.lms.course_service.exception.BusinessException;
import com.dev.lms.course_service.exception.ResourceNotFoundException;
import com.dev.lms.course_service.kafka.CourseEventPublisher;
import com.dev.lms.course_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final SectionRepository sectionRepository;
    private final LessonRepository lessonRepository;
    private final VideoContentRepository videoContentRepository;
    private final TransactionTemplate transactionTemplate;
    private final CourseEventPublisher courseEventPublisher;

    public CourseResponse create(CreateCourseRequest req) {
        return transactionTemplate.execute(status -> {
            Course savedCourse = courseRepository.save(Course.builder()
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
                    .build());

            List<SectionResponse> sectionResponses = new ArrayList<>();
            for (CreateSectionRequest sectionReq : req.sections()) {
                Section savedSection = sectionRepository.save(Section.builder()
                        .course(savedCourse)
                        .title(sectionReq.title())
                        .build());

                List<LessonResponse> lessonResponses = new ArrayList<>();
                for (CreateLessonRequest lessonReq : sectionReq.lessons()) {
                    Lesson savedLesson = lessonRepository.save(Lesson.builder()
                            .section(savedSection)
                            .title(lessonReq.title())
                            .lessonType(lessonReq.lessonType())
                            .build());
                    lessonResponses.add(toLessonResponse(savedLesson, savedSection.getSectionId(), null));
                }
                sectionResponses.add(toSectionResponse(savedSection, savedCourse.getCourseId(), lessonResponses));
            }

            return toCourseResponse(savedCourse, sectionResponses);
        });
    }

    public CourseResponse getById(UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        List<SectionResponse> sections = sectionRepository.findByCourse_CourseId(courseId).stream()
                .map(section -> {
                    List<LessonResponse> lessons = lessonRepository
                            .findBySection_SectionId(section.getSectionId()).stream()
                            .map(lesson -> {
                                VideoContent vc = videoContentRepository
                                        .findByLesson_LessonId(lesson.getLessonId()).orElse(null);
                                return toLessonResponse(lesson, section.getSectionId(), vc);
                            })
                            .toList();
                    return toSectionResponse(section, courseId, lessons);
                })
                .toList();

        return toCourseResponse(course, sections);
    }

    public List<CourseResponse> listByInstructor(UUID instructorId) {
        return courseRepository.findByInstructorId(instructorId).stream()
                .map(course -> toCourseResponse(course, List.of()))
                .toList();
    }

    public CourseInfoResponse getCourseInfo(UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));
        return new CourseInfoResponse(
                course.getCourseId(), course.getInstructorId(), course.getTitle(),
                course.getPrice(), course.getIsFree(), course.getStatus().name()
        );
    }

    public List<CourseOverviewResponse> listAllAsOverview(String role, UUID instructorId) {
        List<Course> courses = (role.equals("ADMIN") || role.equals("MANAGER"))
                ? courseRepository.findAll()
                : courseRepository.findByInstructorId(instructorId);
        return courses.stream().map(this::toOverviewFromCourse).toList();
    }

    public void delete(UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        transactionTemplate.execute(status -> {
            sectionRepository.findByCourse_CourseId(courseId).forEach(section -> {
                lessonRepository.findBySection_SectionId(section.getSectionId()).forEach(lesson -> {
                    videoContentRepository.findByLesson_LessonId(lesson.getLessonId())
                            .ifPresent(videoContentRepository::delete);
                    lessonRepository.delete(lesson);
                });
                sectionRepository.delete(section);
            });
            courseRepository.delete(course);
            return null;
        });

        courseEventPublisher.publish(new CourseIndexEvent(
                CourseIndexEvent.DELETED,
                courseId.toString(),
                null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null
        ));
    }

    private CourseOverviewResponse toOverviewFromCourse(Course c) {
        List<SectionOverviewResponse> sections = sectionRepository.findByCourse_CourseId(c.getCourseId()).stream()
                .map(s -> {
                    List<LessonOverviewResponse> lessons = lessonRepository
                            .findBySection_SectionId(s.getSectionId()).stream()
                            .map(l -> {
                                VideoContent vc = videoContentRepository
                                        .findByLesson_LessonId(l.getLessonId()).orElse(null);
                                return new LessonOverviewResponse(
                                        l.getLessonId().toString(), l.getTitle(),
                                        l.getLessonType().name(),
                                        vc != null ? vc.getVideoUrl() : null,
                                        l.getTextUrl(),
                                        vc != null ? vc.getDurationMinutes() : null,
                                        vc != null ? vc.getIsPreview() : null);
                            })
                            .toList();
                    return new SectionOverviewResponse(s.getSectionId().toString(), s.getTitle(), lessons);
                })
                .toList();
        return new CourseOverviewResponse(
                c.getCourseId().toString(), c.getInstructorId(), c.getTitle(), c.getSubTitle(),
                c.getDescription(), c.getPrice(), c.getIsFree(), c.getLanguage(),
                c.getCouponCode(), c.getRating(), c.getCourseDurationMinutes(), c.getLevel().name(),
                c.getStatus().name(), "POSTGRES", null,
                c.getCreatedAt(), null, null, null,
                sections);
    }

    private CourseResponse toCourseResponse(Course c, List<SectionResponse> sections) {
        return new CourseResponse(
                c.getCourseId(), c.getInstructorId(), c.getTitle(), c.getSubTitle(),
                c.getDescription(), c.getPrice(), c.getIsFree(), c.getLanguage(),
                c.getCouponCode(), c.getRating(), c.getCourseDurationMinutes(),
                c.getStatus().name(), c.getLevel().name(), sections, c.getCreatedAt()
        );
    }

    private SectionResponse toSectionResponse(Section s, UUID courseId, List<LessonResponse> lessons) {
        return new SectionResponse(s.getSectionId(), courseId, s.getTitle(), lessons);
    }

    private LessonResponse toLessonResponse(Lesson l, UUID sectionId, VideoContent vc) {
        return new LessonResponse(
                l.getLessonId(), sectionId, l.getTitle(), l.getLessonType().name(),
                vc != null ? vc.getVideoUrl() : null,
                l.getTextUrl(),
                vc != null ? vc.getDurationMinutes() : null,
                vc != null ? vc.getIsPreview() : null,
                l.getCreatedAt()
        );
    }
}
