package com.redis.triage.model.feign;

import lombok.Data;

import java.util.List;

/**
 * Response DTO for LiteLLM embeddings API
 */
@Data
public class LiteLLMEmbeddingResponse {
    private List<EmbeddingData> data;
    private String model;
    private Usage usage;

    @Data
    public static class EmbeddingData {
        private String object;
        private List<Double> embedding;
        private Integer index;
    }

    @Data
    public static class Usage {
        private Integer promptTokens;
        private Integer totalTokens;
    }
}
