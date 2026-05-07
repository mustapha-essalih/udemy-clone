package com.dev.lms.payment_service.kafka;

import com.dev.lms.payment_service.event.EnrollmentCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnrollmentEventPublisher {

    private final KafkaTemplate<String, EnrollmentCreatedEvent> kafkaTemplate;

    @Value("${payment.topics.enrollment-events}")
    private String enrollmentEventsTopic;

    public void publishEnrollmentCreated(EnrollmentCreatedEvent event) {
        kafkaTemplate.send(enrollmentEventsTopic, event.userId().toString(), event);
        log.info("Published enrollment.created for user {} course {}", event.userId(), event.courseId());
    }
}
