package com.redis.triage.config.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Slack Feign client
 */
@Configuration
public class SlackFeignConfig {

    @Bean
    public RequestInterceptor slackRequestInterceptor() {
        return template -> template.header("Content-Type", "application/json");
    }
}
