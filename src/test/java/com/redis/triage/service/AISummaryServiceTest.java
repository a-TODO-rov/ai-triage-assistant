package com.redis.triage.service;

import com.redis.triage.model.GitHubIssue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AISummaryService
 */
@ExtendWith(MockitoExtension.class)
class AISummaryServiceTest {

    @Mock
    private LiteLLMClient liteLLMClient;

    private AISummaryService aiSummaryService;

    @BeforeEach
    void setUp() {
        aiSummaryService = new AISummaryService(liteLLMClient);
    }

    @Test
    void testGenerateSummary() {
        // Arrange
        GitHubIssue issue = GitHubIssue.builder()
                .id(123L)
                .title("Connection timeout in Redis Cluster")
                .body("I'm experiencing connection timeouts when using Redis Cluster with high load.")
                .build();

        List<String> labels = List.of("bug", "redis-cluster", "performance");

        // Mock LiteLLM response
        when(liteLLMClient.callLLM(anyString())).thenReturn(
                "The user is experiencing connection timeouts in Redis Cluster under high load conditions."
        );

        // Act
        String summary = aiSummaryService.generateSummary(issue, labels);

        // Assert
        assertThat(summary).isNotEmpty();
        assertThat(summary).contains("connection timeouts");
        assertThat(summary).contains("Redis Cluster");
    }

    @Test
    void testGenerateSummaryWithContext() {
        // Arrange
        GitHubIssue issue = GitHubIssue.builder()
                .id(123L)
                .title("Connection timeout in Redis Cluster")
                .body("I'm experiencing connection timeouts when using Redis Cluster with high load.")
                .build();

        List<String> labels = List.of("bug", "redis-cluster", "performance");

        GitHubIssue similarIssue1 = GitHubIssue.builder()
                .id(100L)
                .title("Redis Cluster connection drops under load")
                .build();

        GitHubIssue similarIssue2 = GitHubIssue.builder()
                .id(101L)
                .title("Timeout issues with Redis Cluster")
                .build();

        List<GitHubIssue> similarIssues = List.of(similarIssue1, similarIssue2);

        // Mock LiteLLM response
        when(liteLLMClient.callLLM(anyString())).thenReturn(
                "The user is experiencing connection timeouts in Redis Cluster under high load, similar to previously reported timeout and connection drop issues."
        );

        // Act
        String summary = aiSummaryService.generateSummaryWithContext(issue, labels, similarIssues);

        // Assert
        assertThat(summary).isNotEmpty();
        assertThat(summary).contains("connection timeouts");
        assertThat(summary).contains("Redis Cluster");
        assertThat(summary).contains("similar to previously reported");
    }

    @Test
    void testCleanSummaryResponse() {
        // Arrange
        GitHubIssue issue = GitHubIssue.builder()
                .id(123L)
                .title("Test Issue")
                .body("Test body")
                .build();

        // Test with various AI response formats
        when(liteLLMClient.callLLM(anyString()))
                .thenReturn("Summary: This is a test summary.")
                .thenReturn("\"This is a quoted summary.\"")
                .thenReturn("Here's a summary: The user is reporting a test issue.")
                .thenReturn("The user is reporting a test issue. Summary complete.");

        // Act & Assert
        String summary1 = aiSummaryService.generateSummary(issue, List.of());
        assertThat(summary1).isEqualTo("This is a test summary");

        String summary2 = aiSummaryService.generateSummary(issue, List.of());
        assertThat(summary2).isEqualTo("This is a quoted summary.");

        String summary3 = aiSummaryService.generateSummary(issue, List.of());
        assertThat(summary3).isEqualTo("The user is reporting a test issue");

        String summary4 = aiSummaryService.generateSummary(issue, List.of());
        assertThat(summary4).isEqualTo("The user is reporting a test issue.");
    }

    @Test
    void testHandleErrorGracefully() {
        // Arrange
        GitHubIssue issue = GitHubIssue.builder()
                .id(123L)
                .title("Test Issue")
                .build();

        // Mock LiteLLM to throw exception
        when(liteLLMClient.callLLM(anyString())).thenThrow(new RuntimeException("API error"));

        // Act
        String summary = aiSummaryService.generateSummary(issue, List.of());

        // Assert
        assertThat(summary).isEqualTo("Unable to generate summary at this time.");
    }
}
