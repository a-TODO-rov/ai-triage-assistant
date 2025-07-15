package com.redis.triage.client;

import com.redis.triage.model.feign.LiteLLMChatRequest;
import com.redis.triage.model.feign.LiteLLMChatResponse;
import com.redis.triage.model.feign.LiteLLMEmbeddingRequest;
import com.redis.triage.model.feign.LiteLLMEmbeddingResponse;
import com.redis.triage.config.feign.LiteLLMFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for LiteLLM API
 */
@FeignClient(
    name = "litellm-client",
    url = "${LITELLM_BASE_URL}",
    configuration = LiteLLMFeignConfig.class
)
public interface LiteLLMFeignClient {

    /**
     * Calls the LiteLLM chat completions API
     *
     * @param request The chat completion request
     * @return The chat completion response
     */
    @PostMapping("/v1/chat/completions")
    LiteLLMChatResponse chatCompletions(@RequestBody LiteLLMChatRequest request);

    /**
     * Calls the LiteLLM embeddings API
     *
     * @param request The embedding request
     * @return The embedding response
     */
    @PostMapping("/v1/embeddings")
    LiteLLMEmbeddingResponse embeddings(@RequestBody LiteLLMEmbeddingRequest request);
}
