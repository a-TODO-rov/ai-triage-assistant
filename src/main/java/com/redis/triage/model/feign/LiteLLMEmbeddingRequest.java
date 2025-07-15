package com.redis.triage.model.feign;

import lombok.Builder;
import lombok.Data;

/**
 * Request DTO for LiteLLM embeddings API
 */
@Data
@Builder
public class LiteLLMEmbeddingRequest {
    private String model;
    private String input;
}
