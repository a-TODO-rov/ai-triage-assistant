package com.redis.triage.config.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for GitHub Feign client
 */
@Configuration
public class GitHubFeignConfig {

    @Value("${GITHUB_TOKEN:}")
    private String githubToken;

    @Bean
    public RequestInterceptor githubRequestInterceptor() {
        return template -> {
            template.header("User-Agent", "ai-triage-assistant/1.0");
            template.header("Accept", "application/vnd.github.v3+json");

            if (githubToken != null && !githubToken.isEmpty()) {
                template.header("Authorization", "token " + githubToken);
            }
        };
    }
}
