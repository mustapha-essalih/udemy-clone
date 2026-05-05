package com.dev.lms.admin_service.client;

import com.dev.lms.admin_service.dto.CategoryDto;
import com.dev.lms.admin_service.dto.CourseDraftDto;
import com.dev.lms.admin_service.dto.CreateCategoryRequest;
import com.dev.lms.admin_service.dto.CreateSubCategoryRequest;
import com.dev.lms.admin_service.dto.RejectRequest;
import com.dev.lms.admin_service.dto.SubCategoryDto;
import com.dev.lms.admin_service.dto.UpdateCategoryRequest;
import com.dev.lms.admin_service.dto.UpdateSubCategoryRequest;
import com.dev.lms.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.UUID;

@Component
@RequestScope
public class CourseServiceClient {

    private final WebClient webClient;
    private final HttpServletRequest currentRequest;

    public CourseServiceClient(WebClient.Builder webClientBuilder, HttpServletRequest currentRequest) {
        this.webClient = webClientBuilder.build();
        this.currentRequest = currentRequest;
    }

    private void propagateUserHeaders(HttpHeaders headers) {
        String role = currentRequest.getHeader("X-User-Role");
        String userId = currentRequest.getHeader("X-User-Id");
        String email = currentRequest.getHeader("X-User-Email");
        if (role != null) headers.set("X-User-Role", role);
        if (userId != null) headers.set("X-User-Id", userId);
        if (email != null) headers.set("X-User-Email", email);
    }

    public List<CategoryDto> getAllCategories() {
        return webClient.get()
                .uri("lb://course-service/api/courses/categories")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<CategoryDto>>>() {})
                .map(ApiResponse::data)
                .block();
    }

    public CategoryDto getCategoryById(UUID id) {
        return webClient.get()
                .uri("lb://course-service/api/courses/categories/{id}", id)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<CategoryDto>>() {})
                .map(ApiResponse::data)
                .block();
    }

    public CategoryDto createCategory(String name) {
        return webClient.post()
                .uri("lb://course-service/api/courses/categories")
                .headers(this::propagateUserHeaders)
                .bodyValue(new CreateCategoryRequest(name))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<CategoryDto>>() {})
                .map(ApiResponse::data)
                .block();
    }

    public CategoryDto updateCategory(UUID id, String name) {
        return webClient.put()
                .uri("lb://course-service/api/courses/categories/{id}", id)
                .headers(this::propagateUserHeaders)
                .bodyValue(new UpdateCategoryRequest(name))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<CategoryDto>>() {})
                .map(ApiResponse::data)
                .block();
    }

    public void deleteCategory(UUID id) {
        webClient.delete()
                .uri("lb://course-service/api/courses/categories/{id}", id)
                .headers(this::propagateUserHeaders)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    public List<SubCategoryDto> getAllSubCategories() {
        return webClient.get()
                .uri("lb://course-service/api/courses/subcategories")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<SubCategoryDto>>>() {})
                .map(ApiResponse::data)
                .block();
    }

    public SubCategoryDto getSubCategoryById(UUID id) {
        return webClient.get()
                .uri("lb://course-service/api/courses/subcategories/{id}", id)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<SubCategoryDto>>() {})
                .map(ApiResponse::data)
                .block();
    }

    public List<SubCategoryDto> getSubCategoriesByCategoryId(UUID categoryId) {
        return webClient.get()
                .uri("lb://course-service/api/courses/subcategories/category/{categoryId}", categoryId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<SubCategoryDto>>>() {})
                .map(ApiResponse::data)
                .block();
    }

    public SubCategoryDto createSubCategory(String name, UUID categoryId) {
        return webClient.post()
                .uri("lb://course-service/api/courses/subcategories")
                .headers(this::propagateUserHeaders)
                .bodyValue(new CreateSubCategoryRequest(name, categoryId))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<SubCategoryDto>>() {})
                .map(ApiResponse::data)
                .block();
    }

    public SubCategoryDto updateSubCategory(UUID id, String name) {
        return webClient.put()
                .uri("lb://course-service/api/courses/subcategories/{id}", id)
                .headers(this::propagateUserHeaders)
                .bodyValue(new UpdateSubCategoryRequest(name))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<SubCategoryDto>>() {})
                .map(ApiResponse::data)
                .block();
    }

    public void deleteSubCategory(UUID id) {
        webClient.delete()
                .uri("lb://course-service/api/courses/subcategories/{id}", id)
                .headers(this::propagateUserHeaders)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    public List<CourseDraftDto> getPendingDrafts() {
        return webClient.get()
                .uri("lb://course-service/api/courses/drafts/pending")
                .headers(this::propagateUserHeaders)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<CourseDraftDto>>>() {})
                .map(ApiResponse::data)
                .block();
    }

    public CourseDraftDto getDraft(String draftId) {
        return webClient.get()
                .uri("lb://course-service/api/courses/drafts/{draftId}", draftId)
                .headers(this::propagateUserHeaders)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<CourseDraftDto>>() {})
                .map(ApiResponse::data)
                .block();
    }

    public Object approveDraft(String draftId) {
        return webClient.post()
                .uri("lb://course-service/api/courses/drafts/{draftId}/approve", draftId)
                .headers(this::propagateUserHeaders)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<Object>>() {})
                .map(ApiResponse::data)
                .block();
    }

    public CourseDraftDto rejectDraft(String draftId, String feedback) {
        return webClient.post()
                .uri("lb://course-service/api/courses/drafts/{draftId}/reject", draftId)
                .headers(this::propagateUserHeaders)
                .bodyValue(new RejectRequest(feedback))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<CourseDraftDto>>() {})
                .map(ApiResponse::data)
                .block();
    }

}
