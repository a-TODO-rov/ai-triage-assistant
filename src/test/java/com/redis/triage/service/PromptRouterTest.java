package com.redis.triage.service;

import com.redis.triage.model.LlmRoute;
import com.redis.triage.model.TaskContext;
import com.redis.triage.model.TaskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PromptRouterTest {

    private PromptRouter promptRouter;

    @BeforeEach
    void setUp() {
        promptRouter = new PromptRouter();
    }

    @Test
    void testLabelingTaskRouting() {
        // Arrange
        TaskContext context = new TaskContext(TaskType.LABELING, 1000, "normal", 0.5);

        // Act
        LlmRoute route = promptRouter.routeFor(context);

        // Assert
        assertThat(route.model()).isEqualTo("gpt-4");
        assertThat(route.provider()).isEqualTo("openai");
        assertThat(route.costWeight()).isEqualTo(1.0);
    }

    @Test
    void testSummarizationTaskWithHighTokenCount() {
        // Arrange
        TaskContext context = new TaskContext(TaskType.SUMMARIZATION, 10000, "normal", 0.5);

        // Act
        LlmRoute route = promptRouter.routeFor(context);

        // Assert
        assertThat(route.model()).isEqualTo("us.anthropic.claude-3-5-sonnet-20240620-v1:0");
        assertThat(route.provider()).isEqualTo("anthropic");
        assertThat(route.costWeight()).isEqualTo(0.8);
    }

    @Test
    void testSummarizationTaskWithLowTokenCount() {
        // Arrange
        TaskContext context = new TaskContext(TaskType.SUMMARIZATION, 5000, "normal", 0.5);

        // Act
        LlmRoute route = promptRouter.routeFor(context);

        // Assert
        assertThat(route.model()).isEqualTo("us.anthropic.claude-3-haiku-20240307-v1:0");
        assertThat(route.provider()).isEqualTo("anthropic");
        assertThat(route.costWeight()).isEqualTo(0.2);
    }

    @Test
    void testDefaultTaskWithLowCostSensitivity() {
        // Arrange
        TaskContext context = new TaskContext(TaskType.UNKNOWN, 1000, "normal", 0.2);

        // Act
        LlmRoute route = promptRouter.routeFor(context);

        // Assert
        assertThat(route.model()).isEqualTo("gpt-3.5-turbo");
        assertThat(route.provider()).isEqualTo("openai");
        assertThat(route.costWeight()).isEqualTo(0.1);
    }

    @Test
    void testDefaultTaskWithHighCostSensitivity() {
        // Arrange
        TaskContext context = new TaskContext(TaskType.UNKNOWN, 1000, "normal", 0.8);

        // Act
        LlmRoute route = promptRouter.routeFor(context);

        // Assert
        assertThat(route.model()).isEqualTo("gpt-4");
        assertThat(route.provider()).isEqualTo("openai");
        assertThat(route.costWeight()).isEqualTo(1.0);
    }
}
