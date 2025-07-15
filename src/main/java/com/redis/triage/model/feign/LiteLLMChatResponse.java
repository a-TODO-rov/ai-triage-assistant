package com.redis.triage.model.feign;

import lombok.Data;

import java.util.List;

/**
 * Response DTO for LiteLLM chat completions API
 */
@Data
public class LiteLLMChatResponse {
    private List<Choice> choices;
    private Usage usage;

    @Data
    public static class Choice {
        private Message message;
        private String finishReason;
        private Integer index;
    }

    @Data
    public static class Message {
        private String role;
        private String content;
    }

    @Data
    public static class Usage {
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
    }
}
