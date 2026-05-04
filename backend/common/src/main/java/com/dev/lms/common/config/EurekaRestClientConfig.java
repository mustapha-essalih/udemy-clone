package com.dev.lms.common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.netflix.eureka.http.EurekaClientHttpRequestFactorySupplier;
import org.springframework.cloud.netflix.eureka.http.RestClientDiscoveryClientOptionalArgs;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.security.GeneralSecurityException;

@AutoConfiguration
@ConditionalOnClass(RestClientDiscoveryClientOptionalArgs.class)
public class EurekaRestClientConfig {

    @Bean
    @Primary
    public RestClientDiscoveryClientOptionalArgs restClientDiscoveryClientOptionalArgs(
            EurekaClientHttpRequestFactorySupplier factorySupplier)
            throws GeneralSecurityException, IOException {
        return new RestClientDiscoveryClientOptionalArgs(factorySupplier, RestClient::builder);
    }
}
