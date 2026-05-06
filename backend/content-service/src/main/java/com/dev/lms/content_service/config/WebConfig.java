package com.dev.lms.content_service.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebFluxConfigurer {

    @Value("${content.storage.local.base-path:/home/ahmed-yassine/Desktop/udemy-clone/backend/uploads}")
    private String basePath;

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Paths.get(basePath).toAbsolutePath());
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + Paths.get(basePath).toAbsolutePath() + "/";
        registry.addResourceHandler("/api/content/files/**")
                .addResourceLocations(location);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/content/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders(
                        "Content-Type", "Content-Length",
                        "Content-Range", "Accept-Ranges",
                        "Content-Disposition");
    }
}
