package com.redis.triage.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for WebClient beans
 */
@Configuration
public class WebClientConfig {

    @Value("${app.litellm.base-url:http://localhost:4000}")
    private String liteLLMBaseUrl;

    @Value("${app.litellm.api-key:}")
    private String liteLLMApiKey;

    @Value("${app.slack.webhook-url:}")
    private String slackWebhookUrl;

    /**
     * Creates a WebClient bean configured for LiteLLM API calls
     * 
     * @return Configured WebClient instance
     */
    @Bean("liteLLMWebClient")
    public WebClient liteLLMWebClient() {
        WebClient.Builder builder = WebClient.builder()
                .baseUrl(liteLLMBaseUrl);

        if (!liteLLMApiKey.isEmpty()) {
            builder.defaultHeader("Authorization", "Bearer " + liteLLMApiKey);
        }

        return builder.build();
    }

    /**
     * Creates a WebClient bean configured for Slack webhook calls
     * 
     * @return Configured WebClient instance
     */
    @Bean("slackWebClient")
    public WebClient slackWebClient() {
        return WebClient.builder()
                .baseUrl(slackWebhookUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * Creates a general purpose WebClient bean
     * 
     * @return Configured WebClient instance
     */
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
