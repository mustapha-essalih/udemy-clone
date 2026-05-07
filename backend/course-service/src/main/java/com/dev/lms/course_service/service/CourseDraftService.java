package com.dev.lms.course_service.service;

import com.dev.lms.course_service.draft.CourseDraft;
import com.dev.lms.course_service.draft.DraftStatus;
import com.dev.lms.course_service.draft.LessonDraft;
import com.dev.lms.course_service.draft.SectionDraft;
import com.dev.lms.course_service.dto.*;
import com.dev.lms.course_service.entity.*;
import com.dev.lms.course_service.event.CourseIndexEvent;
import com.dev.lms.course_service.exception.BusinessException;
import com.dev.lms.course_service.exception.ResourceNotFoundException;
import com.dev.lms.course_service.kafka.CourseEventPublisher;
import com.dev.lms.course_service.repository.*;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseDraftService {

    private static final String DRAFT_KEY_PREFIX = "course:draft:";
    private static final String INSTRUCTOR_INDEX_PREFIX = "course:drafts:instructor:";
    private static final String PENDING_INDEX_KEY = "course:drafts:pending";
    private static final Duration DRAFT_TTL = Duration.ofDays(30);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final CourseRepository courseRepository;
    private final SectionRepository sectionRepository;
    private final LessonRepository lessonRepository;
    private final VideoContentRepository videoContentRepository;
    private final TransactionTemplate transactionTemplate;
    private final CourseEventPublisher courseEventPublisher;

    public DraftResponse create(UUID instructorId, CreateDraftRequest req) {
        String draftId = UUID.randomUUID().toString();
        CourseDraft draft = CourseDraft.builder()
                .draftId(draftId)
                .instructorId(instructorId)
                .title(req.title())
                .subTitle(req.subTitle())
                .description(req.description())
                .price(req.price())
                .isFree(req.isFree() != null ? req.isFree() : false)
                .language(req.language())
                .couponCode(req.couponCode())
                .courseDurationMinutes(req.courseDurationMinutes())
                .level(req.level() != null ? req.level().name() : CourseLevel.ALL_LEVELS.name())
                .sections(toSectionDrafts(req.sections()))
                .status(DraftStatus.DRAFT)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        persist(draft);
        addToInstructorIndex(instructorId.toString(), draftId);
        log.info("Draft created: id={} instructorId={}", draftId, instructorId);
        return toResponse(draft);
    }

    public DraftResponse createUpdateDraft(UUID instructorId, UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        if (!course.getInstructorId().equals(instructorId)) {
            throw new AccessDeniedException("Not authorized to create an update draft for this course");
        }
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new BusinessException("Only PUBLISHED courses can have update drafts");
        }

        List<SectionDraft> sectionDrafts = sectionRepository.findByCourse_CourseId(courseId).stream()
                .map(section -> {
                    List<LessonDraft> lessonDrafts = lessonRepository.findBySection_SectionId(section.getSectionId()).stream()
                            .map(lesson -> {
                                VideoContent vc = videoContentRepository.findByLesson_LessonId(lesson.getLessonId()).orElse(null);
                                return LessonDraft.builder()
                                        .lessonId(lesson.getLessonId().toString())
                                        .title(lesson.getTitle())
                                        .lessonType(lesson.getLessonType().name())
                                        .videoUrl(vc != null ? vc.getVideoUrl() : null)
                                        .durationMinutes(vc != null ? vc.getDurationMinutes() : null)
                                        .isPreview(vc != null ? vc.getIsPreview() : null)
                                        .textUrl(lesson.getTextUrl())
                                        .build();
                            })
                            .toList();
                    return SectionDraft.builder()
                            .sectionId(section.getSectionId().toString())
                            .title(section.getTitle())
                            .lessons(new ArrayList<>(lessonDrafts))
                            .build();
                })
                .toList();

        String draftId = UUID.randomUUID().toString();
        CourseDraft draft = CourseDraft.builder()
                .draftId(draftId)
                .instructorId(instructorId)
                .publishedCourseId(courseId)
                .title(course.getTitle())
                .subTitle(course.getSubTitle())
                .description(course.getDescription())
                .price(course.getPrice())
                .isFree(course.getIsFree())
                .language(course.getLanguage())
                .couponCode(course.getCouponCode())
                .courseDurationMinutes(course.getCourseDurationMinutes())
                .level(course.getLevel().name())
                .sections(new ArrayList<>(sectionDrafts))
                .status(DraftStatus.DRAFT)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        persist(draft);
        addToInstructorIndex(instructorId.toString(), draftId);
        return toResponse(draft);
    }

    public DraftResponse getById(String draftId, String role, UUID instructorId) {
        CourseDraft draft = fetch(draftId);

        if (!draft.getInstructorId().equals(instructorId) && !role.equals("ADMIN") && !role.equals("MANAGER")) {
            throw new BusinessException("Not authorized to view this draft");
        }

        return toResponse(draft);
    }

    public List<DraftResponse> listByInstructor(UUID instructorId) {
        String indexKey = INSTRUCTOR_INDEX_PREFIX + instructorId;
        Set<String> draftIds = redis.opsForSet().members(indexKey);
        if (draftIds == null || draftIds.isEmpty()) return List.of();

        List<DraftResponse> result = new ArrayList<>();
        List<String> stale = new ArrayList<>();

        for (String id : draftIds) {
            String json = redis.opsForValue().get(DRAFT_KEY_PREFIX + id);
            if (json == null) {
                stale.add(id);
                continue;
            }
            result.add(toResponse(deserialize(json)));
        }

        if (!stale.isEmpty()) {
            redis.opsForSet().remove(indexKey, stale.toArray(new Object[0]));
        }
        return result;
    }

    public List<DraftResponse> listPending() {
        Set<String> draftIds = redis.opsForSet().members(PENDING_INDEX_KEY);
        if (draftIds == null || draftIds.isEmpty()) return List.of();

        List<DraftResponse> result = new ArrayList<>();
        List<String> stale = new ArrayList<>();

        for (String id : draftIds) {
            String json = redis.opsForValue().get(DRAFT_KEY_PREFIX + id);
            if (json == null) {
                stale.add(id);
                continue;
            }
            CourseDraft draft = deserialize(json);
            if (draft.getStatus() == DraftStatus.PENDING_REVIEW) {
                result.add(toResponse(draft));
            } else {
                stale.add(id);
            }
        }

        if (!stale.isEmpty()) {
            redis.opsForSet().remove(PENDING_INDEX_KEY, stale.toArray(new Object[0]));
        }
        return result;
    }

    public DraftResponse update(String draftId, UUID instructorId, UpdateDraftRequest req) {
        CourseDraft draft = fetch(draftId);

        if (!draft.getInstructorId().equals(instructorId)) {
            throw new BusinessException("Not authorized to update this draft");
        }
        if (draft.getStatus() != DraftStatus.DRAFT && draft.getStatus() != DraftStatus.REJECTED) {
            throw new BusinessException("Only DRAFT or REJECTED courses can be updated");
        }

        if (req.title() != null) draft.setTitle(req.title());
        if (req.subTitle() != null) draft.setSubTitle(req.subTitle());
        if (req.description() != null) draft.setDescription(req.description());
        if (req.price() != null) draft.setPrice(req.price());
        if (req.isFree() != null) draft.setIsFree(req.isFree());
        if (req.language() != null) draft.setLanguage(req.language());
        if (req.couponCode() != null) draft.setCouponCode(req.couponCode());
        if (req.courseDurationMinutes() != null) draft.setCourseDurationMinutes(req.courseDurationMinutes());
        if (req.level() != null) draft.setLevel(req.level().name());
        if (req.sections() != null) draft.setSections(toSectionDrafts(req.sections()));
        draft.setUpdatedAt(LocalDateTime.now());

        if (draft.getStatus() == DraftStatus.REJECTED) {
            draft.setStatus(DraftStatus.DRAFT);
            draft.setRejectionFeedback(null);
        }

        persist(draft);
        return toResponse(draft);
    }

    public DraftResponse submit(String draftId, UUID instructorId) {
        CourseDraft draft = fetch(draftId);

        if (!draft.getInstructorId().equals(instructorId)) {
            throw new BusinessException("Not authorized to submit this draft");
        }
        if (draft.getStatus() != DraftStatus.DRAFT) {
            throw new BusinessException("Only DRAFT courses can be submitted for review");
        }

        draft.setStatus(DraftStatus.PENDING_REVIEW);
        draft.setSubmittedAt(LocalDateTime.now());
        draft.setUpdatedAt(LocalDateTime.now());

        persist(draft);
        redis.opsForSet().add(PENDING_INDEX_KEY, draftId);

        return toResponse(draft);
    }

    public CourseResponse approve(String draftId) {
        CourseDraft draft = fetch(draftId);

        if (draft.getStatus() != DraftStatus.PENDING_REVIEW) {
            throw new BusinessException("Draft '" + draftId + "' has status " + draft.getStatus()
                    + " — only PENDING_REVIEW drafts can be approved");
        }

        CourseLevel level;
        try {
            level = draft.getLevel() != null ? CourseLevel.valueOf(draft.getLevel()) : CourseLevel.ALL_LEVELS;
        } catch (IllegalArgumentException e) {
            level = CourseLevel.ALL_LEVELS;
        }
        final CourseLevel resolvedLevel = level;
        final boolean isUpdate = draft.getPublishedCourseId() != null;

        CourseResponse response = transactionTemplate.execute(status -> {
            Course saved;

            if (isUpdate) {
                Course existing = courseRepository.findById(draft.getPublishedCourseId())
                        .orElseThrow(() -> new ResourceNotFoundException("Course", draft.getPublishedCourseId()));
                existing.setTitle(draft.getTitle());
                existing.setSubTitle(draft.getSubTitle());
                existing.setDescription(draft.getDescription());
                existing.setPrice(draft.getPrice());
                existing.setIsFree(draft.getIsFree() != null ? draft.getIsFree() : false);
                existing.setLanguage(draft.getLanguage());
                existing.setCouponCode(draft.getCouponCode());
                existing.setCourseDurationMinutes(draft.getCourseDurationMinutes());
                existing.setLevel(resolvedLevel);
                existing.setStatus(CourseStatus.PUBLISHED);
                existing.getSections().clear();
                saved = courseRepository.saveAndFlush(existing);
            } else {
                saved = courseRepository.save(Course.builder()
                        .instructorId(draft.getInstructorId())
                        .title(draft.getTitle())
                        .subTitle(draft.getSubTitle())
                        .description(draft.getDescription())
                        .price(draft.getPrice())
                        .isFree(draft.getIsFree() != null ? draft.getIsFree() : false)
                        .language(draft.getLanguage())
                        .couponCode(draft.getCouponCode())
                        .courseDurationMinutes(draft.getCourseDurationMinutes())
                        .level(resolvedLevel)
                        .status(CourseStatus.PUBLISHED)
                        .build());
            }

            List<SectionResponse> sectionResponses = new ArrayList<>();
            for (SectionDraft sd : draft.getSections()) {
                Section section = sectionRepository.save(
                        Section.builder()
                                .course(saved)
                                .title(sd.getTitle())
                                .build());

                List<LessonResponse> lessonResponses = new ArrayList<>();
                for (LessonDraft ld : sd.getLessons()) {
                    Lesson lesson = lessonRepository.save(
                            Lesson.builder()
                                    .section(section)
                                    .title(ld.getTitle())
                                    .lessonType(LessonType.valueOf(ld.getLessonType()))
                                    .textUrl(ld.getTextUrl())
                                    .build());
                    if (ld.getVideoUrl() != null) {
                        videoContentRepository.save(VideoContent.builder()
                                .lesson(lesson)
                                .videoUrl(ld.getVideoUrl())
                                .durationMinutes(ld.getDurationMinutes())
                                .isPreview(ld.getIsPreview() != null ? ld.getIsPreview() : false)
                                .build());
                    }
                    lessonResponses.add(new LessonResponse(
                            lesson.getLessonId(), section.getSectionId(), lesson.getTitle(),
                            lesson.getLessonType().name(), ld.getVideoUrl(), ld.getTextUrl(),
                            ld.getDurationMinutes(), ld.getIsPreview(), lesson.getCreatedAt()));
                }
                sectionResponses.add(new SectionResponse(
                        section.getSectionId(), saved.getCourseId(), section.getTitle(), lessonResponses));
            }

            return new CourseResponse(
                    saved.getCourseId(), saved.getInstructorId(), saved.getTitle(), saved.getSubTitle(),
                    saved.getDescription(), saved.getPrice(), saved.getIsFree(), saved.getLanguage(),
                    saved.getCouponCode(), saved.getRating(), saved.getCourseDurationMinutes(),
                    saved.getStatus().name(), saved.getLevel().name(), sectionResponses, saved.getCreatedAt());
        });

        if (response == null) {
            throw new BusinessException("Approval transaction produced no result for draft: " + draftId);
        }

        redis.delete(DRAFT_KEY_PREFIX + draftId);
        redis.opsForSet().remove(PENDING_INDEX_KEY, draftId);
        redis.opsForSet().remove(INSTRUCTOR_INDEX_PREFIX + draft.getInstructorId().toString(), draftId);

        String eventType = isUpdate ? CourseIndexEvent.UPDATED : CourseIndexEvent.CREATED;
        courseEventPublisher.publish(toIndexEvent(eventType, response));
        log.info("Draft {} approved, course {} published (eventType={})", draftId, response.courseId(), eventType);

        return response;
    }

    public DraftResponse reject(String draftId, String feedback) {
        CourseDraft draft = fetch(draftId);

        if (draft.getStatus() != DraftStatus.PENDING_REVIEW) {
            throw new BusinessException("Draft '" + draftId + "' has status " + draft.getStatus()
                    + " — only PENDING_REVIEW drafts can be rejected");
        }

        draft.setStatus(DraftStatus.REJECTED);
        draft.setRejectionFeedback(feedback);
        draft.setReviewedAt(LocalDateTime.now());
        draft.setUpdatedAt(LocalDateTime.now());

        persist(draft);
        redis.opsForSet().remove(PENDING_INDEX_KEY, draftId);
        log.info("Draft {} rejected", draftId);
        return toResponse(draft);
    }

    public DraftResponse updateLessonVideoUrl(String draftId, String sectionId, String lessonId,
            UpdateVideoContentRequest req) {
        CourseDraft draft = fetch(draftId);
        draft.getSections().stream()
                .filter(s -> sectionId.equals(s.getSectionId()))
                .flatMap(s -> s.getLessons().stream())
                .filter(l -> lessonId.equals(l.getLessonId()))
                .findFirst()
                .ifPresent(l -> {
                    l.setVideoUrl(req.videoUrl());
                    l.setDurationMinutes(req.durationMinutes());
                    l.setIsPreview(req.isPreview());
                });
        draft.setUpdatedAt(LocalDateTime.now());
        persist(draft);
        return toResponse(draft);
    }

    public DraftResponse updateLessonTextUrl(String draftId, String sectionId, String lessonId,
            UpdateTextContentRequest req) {
        CourseDraft draft = fetch(draftId);
        draft.getSections().stream()
                .filter(s -> sectionId.equals(s.getSectionId()))
                .flatMap(s -> s.getLessons().stream())
                .filter(l -> lessonId.equals(l.getLessonId()))
                .findFirst()
                .ifPresent(l -> l.setTextUrl(req.contentUrl()));
        draft.setUpdatedAt(LocalDateTime.now());
        persist(draft);
        return toResponse(draft);
    }

    public List<CourseOverviewResponse> listAllDrafts(String role, UUID instructorId) {
        if (role.equals("ADMIN") || role.equals("MANAGER")) {
            return scanAllDrafts();
        }
        return listDraftsByInstructor(instructorId);
    }

    private List<CourseOverviewResponse> scanAllDrafts() {
        Set<String> draftIds = redis.opsForSet().members(PENDING_INDEX_KEY);
        if (draftIds == null || draftIds.isEmpty()) return List.of();
        List<CourseOverviewResponse> result = new ArrayList<>();
        List<String> stale = new ArrayList<>();
        for (String id : draftIds) {
            String json = redis.opsForValue().get(DRAFT_KEY_PREFIX + id);
            if (json == null) {
                stale.add(id);
                continue;
            }
            result.add(toOverviewFromDraft(deserialize(json)));
        }
        if (!stale.isEmpty()) {
            redis.opsForSet().remove(PENDING_INDEX_KEY, stale.toArray(new Object[0]));
        }
        return result;
    }

    private List<CourseOverviewResponse> listDraftsByInstructor(UUID instructorId) {
        String indexKey = INSTRUCTOR_INDEX_PREFIX + instructorId;
        Set<String> draftIds = redis.opsForSet().members(indexKey);
        if (draftIds == null || draftIds.isEmpty()) return List.of();

        List<CourseOverviewResponse> result = new ArrayList<>();
        List<String> stale = new ArrayList<>();

        for (String id : draftIds) {
            String json = redis.opsForValue().get(DRAFT_KEY_PREFIX + id);
            if (json == null) {
                stale.add(id);
                continue;
            }
            result.add(toOverviewFromDraft(deserialize(json)));
        }

        if (!stale.isEmpty()) {
            redis.opsForSet().remove(indexKey, stale.toArray(new Object[0]));
        }
        return result;
    }

    private CourseOverviewResponse toOverviewFromDraft(CourseDraft d) {
        List<SectionOverviewResponse> sections = d.getSections() == null ? List.of() :
                d.getSections().stream()
                        .map(s -> new SectionOverviewResponse(
                                s.getSectionId(),
                                s.getTitle(),
                                s.getLessons() == null ? List.of() : s.getLessons().stream()
                                        .map(l -> new LessonOverviewResponse(
                                                l.getLessonId(), l.getTitle(), l.getLessonType(),
                                                l.getVideoUrl(), l.getTextUrl(),
                                                l.getDurationMinutes(), l.getIsPreview()))
                                        .toList()))
                        .toList();
        return new CourseOverviewResponse(
                d.getDraftId(), d.getInstructorId(), d.getTitle(), d.getSubTitle(),
                d.getDescription(), d.getPrice(), d.getIsFree(), d.getLanguage(),
                d.getCouponCode(), null, d.getCourseDurationMinutes(), d.getLevel(),
                d.getStatus().name(), "REDIS", d.getRejectionFeedback(),
                d.getCreatedAt(), d.getUpdatedAt(), d.getSubmittedAt(), d.getReviewedAt(),
                sections);
    }

    private CourseDraft fetch(String draftId) {
        String json = redis.opsForValue().get(DRAFT_KEY_PREFIX + draftId);
        if (json == null) throw new ResourceNotFoundException(
                "Draft not found: '" + draftId + "'. It may have already been approved or expired.");
        return deserialize(json);
    }

    private void persist(CourseDraft draft) {
        redis.opsForValue().set(DRAFT_KEY_PREFIX + draft.getDraftId(),
                objectMapper.writeValueAsString(draft), DRAFT_TTL);
    }

    private void addToInstructorIndex(String instructorId, String draftId) {
        String indexKey = INSTRUCTOR_INDEX_PREFIX + instructorId;
        redis.opsForSet().add(indexKey, draftId);
        redis.expire(indexKey, DRAFT_TTL);
    }

    private CourseDraft deserialize(String json) {
        return objectMapper.readValue(json, CourseDraft.class);
    }

    private List<SectionDraft> toSectionDrafts(List<CreateSectionRequest> sections) {
        if (sections == null) return List.of();
        return sections.stream()
                .map(s -> SectionDraft.builder()
                        .sectionId(UUID.randomUUID().toString())
                        .title(s.title())
                        .lessons(s.lessons().stream()
                                .map(l -> LessonDraft.builder()
                                        .lessonId(UUID.randomUUID().toString())
                                        .title(l.title())
                                        .lessonType(l.lessonType().name())
                                        .build())
                                .toList())
                        .build())
                .toList();
    }

    private DraftResponse toResponse(CourseDraft d) {
        return new DraftResponse(
                d.getDraftId(), d.getInstructorId(), d.getTitle(), d.getSubTitle(),
                d.getDescription(), d.getPrice(), d.getIsFree(), d.getLanguage(),
                d.getCouponCode(), d.getCourseDurationMinutes(), d.getLevel(),
                d.getSections(), d.getStatus().name(), d.getRejectionFeedback(),
                d.getCreatedAt(), d.getUpdatedAt(), d.getSubmittedAt(), d.getReviewedAt());
    }

    private CourseIndexEvent toIndexEvent(String eventType, CourseResponse r) {
        return new CourseIndexEvent(
                eventType,
                r.courseId().toString(),
                r.title(),
                r.description(),
                r.instructorId().toString(),
                null,
                null,
                null,
                r.rating() != null ? r.rating().floatValue() : null,
                null,
                null,
                r.language(),
                r.courseDurationMinutes(),
                r.price() != null ? r.price().floatValue() : null,
                r.isFree(),
                r.level(),
                r.status(),
                r.createdAt() != null ? r.createdAt().toInstant(ZoneOffset.UTC) : null,
                Instant.now(),
                null
        );
    }

    public void delete(String draftId, UUID instructorId) {
        CourseDraft draft = fetch(draftId);
        if (!draft.getInstructorId().equals(instructorId)) {
            throw new AccessDeniedException("You are not authorized to delete this draft.");
        }
        redis.delete(DRAFT_KEY_PREFIX + draftId);
        redis.opsForSet().remove(INSTRUCTOR_INDEX_PREFIX + instructorId, draftId);
    }
}
