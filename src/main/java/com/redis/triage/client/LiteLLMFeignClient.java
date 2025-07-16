package com.redis.triage.client;

import com.redis.triage.model.feign.LiteLLMChatRequest;
import com.redis.triage.model.feign.LiteLLMChatResponse;
import com.redis.triage.model.feign.LiteLLMEmbeddingRequest;
import com.redis.triage.model.feign.LiteLLMEmbeddingResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Feign client for LiteLLM API
 */
@FeignClient(
    name = "litellm-client",
    url = "${LITELLM_BASE_URL}"
)
public interface LiteLLMFeignClient {

    /**
     * Calls the LiteLLM chat completions API
     *
     * @param request The chat completion request
     * @param authorization The Authorization header
     * @return The chat completion response
     */
    @PostMapping("/v1/chat/completions")
    LiteLLMChatResponse chatCompletions(
        @RequestBody LiteLLMChatRequest request,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Calls the LiteLLM embeddings API
     *
     * @param request The embedding request
     * @param authorization The Authorization header
     * @return The embedding response
     */
    @PostMapping("/v1/embeddings")
    LiteLLMEmbeddingResponse embeddings(
        @RequestBody LiteLLMEmbeddingRequest request,
        @RequestHeader("Authorization") String authorization
    );
}
