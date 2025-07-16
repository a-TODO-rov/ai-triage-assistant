package com.redis.triage.model;

/**
 * Context information for routing LLM requests
 *
 * @param taskType The type of task (e.g., "labeling", "summarization")
 * @param tokenCount Estimated token count for the request
 * @param urgency The urgency level of the task
 * @param costSensitivity Cost sensitivity factor (0.0 = cost-insensitive, 1.0 = highly cost-sensitive)
 */
public record TaskContext(
    TaskType taskType,
    int tokenCount,
    String urgency,
    double costSensitivity
) {
}
