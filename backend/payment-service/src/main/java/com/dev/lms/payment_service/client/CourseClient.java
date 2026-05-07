package com.dev.lms.payment_service.client;

import com.dev.lms.common.response.ApiResponse;
import com.dev.lms.payment_service.dto.CourseInfoDto;
import com.dev.lms.payment_service.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Slf4j
@Component
public class CourseClient {

    private final RestClient restClient;

    public CourseClient(@Qualifier("loadBalancedRestClientBuilder") RestClient.Builder builder) {
        this.restClient = builder.baseUrl("http://course-service").build();
    }

    public CourseInfoDto getCourseInfo(UUID courseId, String userId) {
        try {
            ApiResponse<CourseInfoDto> response = restClient.get()
                    .uri("/api/courses/{courseId}/info", courseId)
                    .header("X-User-Id", userId)
                    .header("X-User-Role", "STUDENT")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || response.data() == null) {
                throw new BusinessException("Course not found: " + courseId);
            }
            return response.data();
        } catch (RestClientException e) {
            log.error("Failed to fetch course info for {}: {}", courseId, e.getMessage());
            throw new BusinessException("Unable to retrieve course information");
        }
    }
}
