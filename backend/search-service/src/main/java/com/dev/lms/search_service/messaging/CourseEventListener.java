package com.dev.lms.search_service.messaging;

import com.dev.lms.search_service.event.CourseIndexEvent;
import com.dev.lms.search_service.service.CourseIndexService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CourseEventListener {

    private static final Logger log = LoggerFactory.getLogger(CourseEventListener.class);

    private final CourseIndexService indexService;

    public CourseEventListener(CourseIndexService indexService) {
        this.indexService = indexService;
    }

    @KafkaListener(topics = "${search.topics.course-events:course.events}", groupId = "${spring.kafka.consumer.group-id}")
    public void onCourseEvent(CourseIndexEvent event) {
        log.info("Received course event: type={} courseId={}", event.eventType(), event.courseId());
        try {
            indexService.index(event);
        } catch (Exception ex) {
            log.error("Failed to index course event for {}", event.courseId(), ex);
            throw ex;
        }
    }
}
