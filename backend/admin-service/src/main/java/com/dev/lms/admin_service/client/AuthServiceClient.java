package com.dev.lms.admin_service.client;
 
import com.dev.lms.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.reactive.function.client.WebClient;

import com.dev.lms.common.request.RegisterRequest;
import com.dev.lms.common.response.RegistrationResponse;


@Component
@RequestScope
public class AuthServiceClient {

    private final WebClient webClient;
    private final HttpServletRequest currentRequest;

    public AuthServiceClient(WebClient.Builder webClientBuilder, HttpServletRequest currentRequest) {
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

    public RegistrationResponse registerManager(RegisterRequest request) {
        return webClient.post()
                .uri("lb://user-service/api/auth/register-manager")
                .headers(this::propagateUserHeaders)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<RegistrationResponse>>() {})
                .map(ApiResponse::data)
                .block();
    }
}
