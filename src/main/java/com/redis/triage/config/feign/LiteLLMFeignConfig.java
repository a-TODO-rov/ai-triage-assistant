package com.redis.triage.config.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for LiteLLM Feign client
 */
@Configuration
public class LiteLLMFeignConfig {

    @Value("${LITELLM_API_KEY}")
    private String liteLLMApiKey;

    @Bean
    public RequestInterceptor liteLLMRequestInterceptor() {
        return template -> {
            if (liteLLMApiKey != null && !liteLLMApiKey.isEmpty()) {
                template.header("Authorization", "Bearer " + liteLLMApiKey);
            }
            template.header("Content-Type", "application/json");
        };
    }
}
