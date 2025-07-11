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
     * Generates embeddings for the given text using LiteLLM API
     *
     * @param text The text to generate embeddings for
     * @return The embedding vector as float array
     */
    public float[] generateEmbedding(String text) {
        log.info("Generating embedding for text using LiteLLM API");
        log.debug("Text content: {}", text);

        try {
            // Prepare the request payload for embeddings
            Map<String, Object> requestBody = Map.of(
                "model", "text-embedding-ada-002",
                "input", text
            );

            // Make the API call to embeddings endpoint
            Mono<Map> responseMono = liteLLMWebClient
                .post()
                .uri("/v1/embeddings")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class);

            Map<String, Object> response = responseMono.block();

            if (response == null) {
                log.error("Received null response from LiteLLM embeddings API");
                return new float[0];
            }

            // Extract the embedding from the response
            float[] embedding = extractEmbeddingFromResponse(response);
            log.info("Successfully generated embedding with {} dimensions", embedding.length);

            return embedding;

        } catch (Exception e) {
            log.error("Error generating embedding from LiteLLM API: {}", e.getMessage(), e);
            return new float[0];
        }
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
     * Extracts the embedding from the LiteLLM embeddings API response
     *
     * @param response The response map from the embeddings API
     * @return The extracted embedding as float array
     */
    @SuppressWarnings("unchecked")
    private float[] extractEmbeddingFromResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
            if (data != null && !data.isEmpty()) {
                Map<String, Object> firstEmbedding = data.get(0);
                List<Double> embeddingList = (List<Double>) firstEmbedding.get("embedding");
                if (embeddingList != null) {
                    float[] embedding = new float[embeddingList.size()];
                    for (int i = 0; i < embeddingList.size(); i++) {
                        embedding[i] = embeddingList.get(i).floatValue();
                    }
                    return embedding;
                }
            }
            log.warn("Unexpected embeddings response structure from LiteLLM API: {}", response);
            return new float[0];
        } catch (Exception e) {
            log.error("Error extracting embedding from response: {}", e.getMessage(), e);
            return new float[0];
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
