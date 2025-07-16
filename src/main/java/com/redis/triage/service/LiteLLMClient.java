package com.redis.triage.service;

import com.redis.triage.client.LiteLLMFeignClient;
import com.redis.triage.model.feign.LiteLLMChatRequest;
import com.redis.triage.model.feign.LiteLLMChatResponse;
import com.redis.triage.model.feign.LiteLLMEmbeddingRequest;
import com.redis.triage.model.feign.LiteLLMEmbeddingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for interacting with LiteLLM API
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LiteLLMClient {

    private final LiteLLMFeignClient liteLLMFeignClient;

    @Value("${LITELLM_API_KEY:}")
    private String liteLLMApiKey;

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
            // Prepare the request
            LiteLLMEmbeddingRequest request = LiteLLMEmbeddingRequest.builder()
                .model("text-embedding-3-small")
                .input(text)
                .build();

            // Make the API call
            LiteLLMEmbeddingResponse response = liteLLMFeignClient.embeddings(
                request,
                liteLLMApiKey != null && !liteLLMApiKey.isEmpty() ? "Bearer " + liteLLMApiKey : ""
            );

            if (response == null || response.getData() == null || response.getData().isEmpty()) {
                log.error("Received null or empty response from LiteLLM embeddings API");
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
     * Calls the LiteLLM API with the given prompt and specified model
     *
     * @param prompt The prompt to send to the LLM
     * @param model The model to use (e.g., "gpt-4", "claude-3-opus")
     * @param provider The provider (e.g., "openai", "anthropic")
     * @return The raw string content of the LLM response
     */
    public String callLLM(String prompt, String model, String provider) {
        log.info("Sending prompt to LiteLLM API using model: {} from provider: {}", model, provider);
        log.debug("Prompt content: {}", prompt);

        try {
            // Prepare the request
            LiteLLMChatRequest.ChatMessage message = LiteLLMChatRequest.ChatMessage.builder()
                .role("user")
                .content(prompt)
                .build();

            LiteLLMChatRequest request = LiteLLMChatRequest.builder()
                .model(model)
                .messages(List.of(message))
                .temperature(0.3)
                .build();

            // Make the API call
            LiteLLMChatResponse response = liteLLMFeignClient.chatCompletions(
                request,
                liteLLMApiKey != null && !liteLLMApiKey.isEmpty() ? "Bearer " + liteLLMApiKey : ""
            );

            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                log.error("Received null or empty response from LiteLLM API");
                return "Error: No response from LiteLLM API";
            }

            // Extract the content from the response
            String content = extractContentFromResponse(response);
            log.info("Successfully received response from LiteLLM API using model: {}", model);
            log.debug("Response content: {}", content);

            return content;

        } catch (Exception e) {
            log.error("Error calling LiteLLM API with model {}: {}", model, e.getMessage(), e);
            return "Error: Failed to get response from LiteLLM API - " + e.getMessage();
        }
    }

    /**
     * Extracts the embedding from the LiteLLM embeddings API response
     *
     * @param response The response from the embeddings API
     * @return The extracted embedding as float array
     */
    private float[] extractEmbeddingFromResponse(LiteLLMEmbeddingResponse response) {
        try {
            if (response.getData() != null && !response.getData().isEmpty()) {
                LiteLLMEmbeddingResponse.EmbeddingData firstEmbedding = response.getData().get(0);
                List<Double> embeddingList = firstEmbedding.getEmbedding();
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
     * @param response The response from the API
     * @return The extracted content string
     */
    private String extractContentFromResponse(LiteLLMChatResponse response) {
        try {
            if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                LiteLLMChatResponse.Choice firstChoice = response.getChoices().get(0);
                LiteLLMChatResponse.Message message = firstChoice.getMessage();
                if (message != null && message.getContent() != null) {
                    return message.getContent();
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
