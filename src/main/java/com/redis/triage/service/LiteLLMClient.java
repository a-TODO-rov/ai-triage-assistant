package com.redis.triage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Service for interacting with LiteLLM API
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LiteLLMClient {

    @Qualifier("liteLLMWebClient")
    private final WebClient liteLLMWebClient;

    /**
     * Sends a prompt to LiteLLM and returns the response
     *
     * @param prompt The prompt to send to LiteLLM
     * @return The response from LiteLLM
     */
    public String sendPrompt(String prompt) {
        return callLLM(prompt);
    }

    /**
     * Calls the LiteLLM API with the given prompt
     *
     * @param prompt The prompt to send to the LLM
     * @return The raw string content of the LLM response
     */
    public String callLLM(String prompt) {
        log.info("Sending prompt to LiteLLM API");
        log.debug("Prompt content: {}", prompt);

        try {
            // Prepare the request payload
            Map<String, Object> requestBody = Map.of(
                "model", "gpt-4",
                "messages", List.of(
                    Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.3
            );

            // Make the API call
            Mono<Map> responseMono = liteLLMWebClient
                .post()
                .uri("/v1/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class);

            Map<String, Object> response = responseMono.block();

            if (response == null) {
                log.error("Received null response from LiteLLM API");
                return "Error: No response from LiteLLM API";
            }

            // Extract the content from the response
            String content = extractContentFromResponse(response);
            log.info("Successfully received response from LiteLLM API");
            log.debug("Response content: {}", content);

            return content;

        } catch (Exception e) {
            log.error("Error calling LiteLLM API: {}", e.getMessage(), e);
            return "Error: Failed to get response from LiteLLM API - " + e.getMessage();
        }
    }

    /**
     * Extracts the content from the LiteLLM API response
     *
     * @param response The response map from the API
     * @return The extracted content string
     */
    @SuppressWarnings("unchecked")
    private String extractContentFromResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> firstChoice = choices.get(0);
                Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                if (message != null) {
                    Object content = message.get("content");
                    return content != null ? content.toString() : "No content in response";
                }
            }
            log.warn("Unexpected response structure from LiteLLM API: {}", response);
            return "Error: Unexpected response structure";
        } catch (Exception e) {
            log.error("Error extracting content from response: {}", e.getMessage(), e);
            return "Error: Failed to parse response";
        }
    }
}
