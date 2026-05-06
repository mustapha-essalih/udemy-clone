package com.dev.lms.course_service.kafka;

import com.dev.lms.course_service.event.CourseIndexEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CourseEventPublisher {

    private final KafkaTemplate<String, CourseIndexEvent> kafkaTemplate;

    @Value("${course.topics.course-events}")
    private String courseEventsTopic;

    public void publish(CourseIndexEvent event) {
        kafkaTemplate.send(courseEventsTopic, event.courseId(), event);
        log.info("Published {} event for course {}", event.eventType(), event.courseId());
    }
}
