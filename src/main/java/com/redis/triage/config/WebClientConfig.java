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

    @Value("${LITELLM_BASE_URL}")
    private String liteLLMBaseUrl;

    @Value("${LITELLM_API_KEY}")
    private String liteLLMApiKey;

    @Value("${SLACK_WEBHOOK_URL}")
    private String slackWebhookUrl;

    @Value("${GITHUB_TOKEN:}")
    private String githubToken;

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
     * Creates a WebClient bean configured for GitHub API calls
     *
     * @return Configured WebClient instance
     */
    @Bean("githubWebClient")
    public WebClient githubWebClient() {
        WebClient.Builder builder = WebClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("User-Agent", "ai-triage-assistant/1.0")
                .defaultHeader("Accept", "application/vnd.github.v3+json");

        // Add authorization header if GitHub token is provided
        if (githubToken != null && !githubToken.isEmpty()) {
            builder.defaultHeader("Authorization", "token " + githubToken);
        }

        return builder.build();
    }
}
