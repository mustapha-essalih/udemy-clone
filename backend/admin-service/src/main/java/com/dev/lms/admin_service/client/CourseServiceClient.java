package com.dev.lms.admin_service.client;

import com.dev.lms.admin_service.dto.CategoryDto;
import com.dev.lms.admin_service.dto.SubCategoryDto;
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

    private String authHeader() {
        return currentRequest.getHeader(HttpHeaders.AUTHORIZATION);
    }

    public List<CategoryDto> getAllCategories() {
        return webClient
                .get()
                .uri("lb://course-service/api/courses/categories")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<CategoryDto>>>() {})
                .map(ApiResponse::data)
                .block();
    }

    public CategoryDto getCategoryById(UUID id) {
        return webClient
                .get()
                .uri("lb://course-service/api/courses/categories/{id}", id)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<CategoryDto>>() {})
                .map(ApiResponse::data)
                .block();
    }

    public CategoryDto createCategory(String name) {
        return webClient
                .post()
                .uri("lb://course-service/api/courses/categories")
                .header(HttpHeaders.AUTHORIZATION, authHeader())
                .bodyValue(new CreateCategoryRequest(name))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<CategoryDto>>() {})
                .map(ApiResponse::data)
                .block();
    }

    public CategoryDto updateCategory(UUID id, String name) {
        return webClient
                .put()
                .uri("lb://course-service/api/courses/categories/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, authHeader())
                .bodyValue(new UpdateCategoryRequest(name))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<CategoryDto>>() {})
                .map(ApiResponse::data)
                .block();
    }

    public void deleteCategory(UUID id) {
        webClient
                .delete()
                .uri("lb://course-service/api/courses/categories/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, authHeader())
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    public List<SubCategoryDto> getAllSubCategories() {
        return webClient
                .get()
                .uri("lb://course-service/api/courses/subcategories")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<SubCategoryDto>>>() {})
                .map(ApiResponse::data)
                .block();
    }

    public SubCategoryDto getSubCategoryById(UUID id) {
        return webClient
                .get()
                .uri("lb://course-service/api/courses/subcategories/{id}", id)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<SubCategoryDto>>() {})
                .map(ApiResponse::data)
                .block();
    }

    public List<SubCategoryDto> getSubCategoriesByCategoryId(UUID categoryId) {
        return webClient
                .get()
                .uri("lb://course-service/api/courses/subcategories/category/{categoryId}", categoryId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<SubCategoryDto>>>() {})
                .map(ApiResponse::data)
                .block();
    }

    public SubCategoryDto createSubCategory(String name, UUID categoryId) {
        return webClient
                .post()
                .uri("lb://course-service/api/courses/subcategories")
                .header(HttpHeaders.AUTHORIZATION, authHeader())
                .bodyValue(new CreateSubCategoryRequest(name, categoryId))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<SubCategoryDto>>() {})
                .map(ApiResponse::data)
                .block();
    }

    public SubCategoryDto updateSubCategory(UUID id, String name) {
        return webClient
                .put()
                .uri("lb://course-service/api/courses/subcategories/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, authHeader())
                .bodyValue(new UpdateSubCategoryRequest(name))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<SubCategoryDto>>() {})
                .map(ApiResponse::data)
                .block();
    }

    public void deleteSubCategory(UUID id) {
        webClient
                .delete()
                .uri("lb://course-service/api/courses/subcategories/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, authHeader())
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    private record CreateCategoryRequest(String name) {}
    private record UpdateCategoryRequest(String name) {}
    private record CreateSubCategoryRequest(String name, UUID categoryId) {}
    private record UpdateSubCategoryRequest(String name) {}
}
