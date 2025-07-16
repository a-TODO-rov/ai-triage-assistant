package com.redis.triage.model;

/**
 * Represents the routing decision for an LLM request
 *
 * @param model The LLM model to use (e.g., "gpt-4", "claude-3-opus")
 * @param provider The LLM provider (e.g., "openai", "anthropic")
 * @param costWeight The relative cost weight of this route (0.0 = cheapest, 1.0 = most expensive)
 */
public record LlmRoute(
    String model,
    String provider,
    double costWeight
) {
}
