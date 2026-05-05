package com.dev.lms.search_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IndexNamesConfig {

    @Bean(name = "searchIndexName")
    public String searchIndexName(@Value("${search.index.courses}") String name) {
        return name;
    }

    @Bean(name = "suggestionIndexName")
    public String suggestionIndexName(@Value("${search.index.suggestions}") String name) {
        return name;
    }
}
