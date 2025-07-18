package com.redis.triage.service;

import com.redis.triage.client.SlackFeignClient;
import com.redis.triage.model.GitHubSimilarIssue;
import com.redis.triage.model.webhook.GitHubIssue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SlackNotifier service
 */
@ExtendWith(MockitoExtension.class)
class SlackNotifierTest {

    @Mock
    private SlackFeignClient slackFeignClient;

    private SlackNotifier slackNotifier;

    @BeforeEach
    void setUp() {
        slackNotifier = new SlackNotifier(slackFeignClient);
    }

    @Test
    void testSendNotificationWithAllFields() {
        // Arrange
        GitHubIssue issue = GitHubIssue.builder()
                .id(123L)
                .number(456)
                .title("Test Issue Title")
                .htmlUrl("https://github.com/test/repo/issues/456")
                .body("This is a test issue body")
                .build();

        List<String> labels = List.of("bug", "redis-cluster", "performance");

        GitHubSimilarIssue similarIssue1 = new GitHubSimilarIssue(GitHubIssue.builder()
                .id(100L)
                .number(100)
                .title("Similar Issue 1")
                .htmlUrl("https://github.com/test/repo/issues/100")
                .build(), 0.3, 80);

        GitHubSimilarIssue similarIssue2 = new GitHubSimilarIssue(GitHubIssue.builder()
                .id(200L)
                .number(200)
                .title("Similar Issue 2")
                .htmlUrl("https://github.com/test/repo/issues/200")
                .build(), 0.5, 70);

        List<GitHubSimilarIssue> similarIssues = List.of(similarIssue1, similarIssue2);

        String aiSummary = "This issue appears to be related to Redis cluster performance.\nThe user is experiencing slow response times.";

        // Mock Feign client
        when(slackFeignClient.sendMessage(any())).thenReturn("ok");

        // Act
        slackNotifier.sendNotification(issue, labels, similarIssues, aiSummary);

        // Assert
        verify(slackFeignClient).sendMessage(any());
    }

    @Test
    void testSendNotificationWithMinimalData() {
        // Arrange
        GitHubIssue issue = GitHubIssue.builder()
                .id(123L)
                .title("Minimal Issue")
                .build();

        // Mock Feign client
        when(slackFeignClient.sendMessage(any())).thenReturn("ok");

        // Act
        slackNotifier.sendNotification(issue, List.of(), List.of(), null);

        // Assert
        verify(slackFeignClient).sendMessage(any());
    }

    @Test
    void testSendNotificationBackwardCompatibility() {
        // Arrange
        GitHubIssue issue = GitHubIssue.builder()
                .id(123L)
                .title("Backward Compatible Issue")
                .htmlUrl("https://github.com/test/repo/issues/123")
                .build();

        List<String> labels = List.of("feature");
        List<GitHubSimilarIssue> similarIssues = List.of();

        // Mock Feign client
        when(slackFeignClient.sendMessage(any())).thenReturn("ok");

        // Act - using the old method signature (without AI summary)
        slackNotifier.sendNotification(issue, labels, similarIssues);

        // Assert
        verify(slackFeignClient).sendMessage(any());
    }
}
