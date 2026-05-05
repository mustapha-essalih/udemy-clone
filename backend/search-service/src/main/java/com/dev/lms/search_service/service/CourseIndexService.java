package com.dev.lms.search_service.service;

import com.dev.lms.search_service.document.CourseDocument;
import com.dev.lms.search_service.document.SearchTermDocument;
import com.dev.lms.search_service.event.CourseIndexEvent;
import com.dev.lms.search_service.repository.CourseSearchRepository;
import com.dev.lms.search_service.repository.SearchTermRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.suggest.Completion;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class CourseIndexService {

    private static final Logger log = LoggerFactory.getLogger(CourseIndexService.class);

    private final CourseSearchRepository courseRepo;
    private final SearchTermRepository termRepo;

    public CourseIndexService(CourseSearchRepository courseRepo, SearchTermRepository termRepo) {
        this.courseRepo = courseRepo;
        this.termRepo = termRepo;
    }

    public void index(CourseIndexEvent event) {
        if (event.courseId() == null) {
            log.warn("Skipping index event with null courseId");
            return;
        }
        if (CourseIndexEvent.DELETED.equals(event.eventType())) {
            courseRepo.deleteById(event.courseId());
            return;
        }
        CourseDocument doc = toDocument(event);
        courseRepo.save(doc);
    }

    public void deleteById(String courseId) {
        courseRepo.deleteById(courseId);
    }

    public void recordSearchTerm(String term) {
        if (term == null) return;
        String normalized = term.trim().toLowerCase();
        if (normalized.isEmpty() || normalized.length() < 2) return;

        String id = UUID.nameUUIDFromBytes(normalized.getBytes()).toString();
        Optional<SearchTermDocument> existing = termRepo.findById(id);
        SearchTermDocument doc = existing.orElseGet(() -> SearchTermDocument.builder()
                .termId(id)
                .term(normalized)
                .frequency(0L)
                .build());
        doc.setFrequency(Optional.ofNullable(doc.getFrequency()).orElse(0L) + 1);
        doc.setLastSearchedAt(Instant.now());
        Completion completion = new Completion(new String[]{normalized});
        completion.setWeight(doc.getFrequency().intValue());
        doc.setTermSuggest(completion);
        termRepo.save(doc);
    }

    private CourseDocument toDocument(CourseIndexEvent e) {
        CourseDocument.CourseDocumentBuilder builder = CourseDocument.builder()
                .courseId(e.courseId())
                .title(e.title())
                .description(e.description())
                .instructorName(e.instructorName())
                .instructorId(e.instructorId())
                .category(e.category())
                .subcategory(e.subcategory())
                .rating(e.rating())
                .ratingCount(e.ratingCount())
                .enrollments(e.enrollments())
                .language(e.language())
                .durationMinutes(e.durationMinutes())
                .price(e.price())
                .isFree(e.isFree())
                .level(e.level())
                .status(e.status())
                .createdAt(e.createdAt())
                .updatedAt(Optional.ofNullable(e.updatedAt()).orElse(Instant.now()))
                .tags(e.tags());

        if (e.title() != null && !e.title().isBlank()) {
            Completion titleCompletion = new Completion(new String[]{e.title()});
            int weight = popularityWeight(e.enrollments(), e.rating());
            titleCompletion.setWeight(weight);
            builder.titleSuggest(titleCompletion);
        }
        if (e.instructorName() != null && !e.instructorName().isBlank()) {
            Completion instructorCompletion = new Completion(new String[]{e.instructorName()});
            instructorCompletion.setWeight(popularityWeight(e.enrollments(), e.rating()));
            builder.instructorSuggest(instructorCompletion);
        }
        return builder.build();
    }

    private int popularityWeight(Long enrollments, Float rating) {
        long base = Optional.ofNullable(enrollments).orElse(0L);
        float r = Optional.ofNullable(rating).orElse(0f);
        long score = base + (long) (r * 100);
        if (score > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) score;
    }
}
