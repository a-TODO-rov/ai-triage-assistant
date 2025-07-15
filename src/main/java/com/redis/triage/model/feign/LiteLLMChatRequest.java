package com.redis.triage.model.feign;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Request DTO for LiteLLM chat completions API
 */
@Data
@Builder
public class LiteLLMChatRequest {
    private String model;
    private List<ChatMessage> messages;
    private Double temperature;

    @Data
    @Builder
    public static class ChatMessage {
        private String role;
        private String content;
    }
}
