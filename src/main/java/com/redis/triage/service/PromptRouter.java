package com.redis.triage.service;

import com.redis.triage.model.LlmRoute;
import com.redis.triage.model.TaskContext;
import com.redis.triage.model.TaskType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for routing LLM requests to appropriate models based on task context
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PromptRouter {

    /**
     * Routes a task to the appropriate LLM model based on context
     *
     * @param context The task context containing type, token count, urgency, and cost sensitivity
     * @return The routing decision with model, provider, and cost weight
     */
    public LlmRoute routeFor(TaskContext context) {
        log.debug("Routing request for task: {}, tokens: {}, urgency: {}, cost sensitivity: {}",
            context.taskType(), context.tokenCount(), context.urgency(), context.costSensitivity());

        LlmRoute route = switch (context.taskType()) {
            case TaskType.LABELING -> new LlmRoute("gpt-4", "openai", 1.0);
            case TaskType.SUMMARIZATION -> {
                if (context.tokenCount() > 8000) {
                    yield new LlmRoute("us.anthropic.claude-3-5-sonnet-20240620-v1:0", "anthropic", 0.8);
                }
                yield new LlmRoute("us.anthropic.claude-3-haiku-20240307-v1:0", "anthropic", 0.2);
            }
            default -> {
                if (context.costSensitivity() < 0.3) {
                    yield new LlmRoute("gpt-3.5-turbo", "openai", 0.1);
                }
                yield new LlmRoute("gpt-4", "openai", 1.0);
            }
        };

        // Log the routing decision
        log.info("Routing task: {}, tokens: {}, route: {}", 
            context.taskType(), context.tokenCount(), route);

        return route;
    }
}
