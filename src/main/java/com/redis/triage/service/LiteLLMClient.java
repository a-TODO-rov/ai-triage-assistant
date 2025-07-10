package com.redis.triage.service;

import org.springframework.stereotype.Service;

/**
 * Service for interacting with LiteLLM API
 */
@Service
public class LiteLLMClient {

    /**
     * Sends a prompt to LiteLLM and returns the response
     * 
     * @param prompt The prompt to send to LiteLLM
     * @return The response from LiteLLM
     */
    public String sendPrompt(String prompt) {
        // TODO: Implement LiteLLM API integration
        return "LiteLLM response placeholder";
    }
}
