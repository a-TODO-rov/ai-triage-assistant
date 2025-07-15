package com.redis.triage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.triage.model.webhook.GitHubIssue;
import com.redis.triage.model.feign.Label;
import com.redis.triage.model.IssueContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import redis.clients.jedis.JedisPooled;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LabelingService
 */
@ExtendWith(MockitoExtension.class)
class LabelingServiceTest {

    @Mock
    private LiteLLMClient liteLLMClient;

    @Mock
    private GitHubService gitHubService;

    @Mock
    private JedisPooled jedis;

    private ObjectMapper objectMapper;
    private LabelingService labelingService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        labelingService = new LabelingService(liteLLMClient, gitHubService, jedis, objectMapper);
    }

    @Test
    void testGenerateLabelsWithCachedLabels() throws Exception {
        // Given
        GitHubIssue issue = GitHubIssue.builder()
                .title("Redis connection timeout")
                .body("Getting timeout errors when connecting to Redis cluster")
                .build();

        String repositoryUrl = "https://api.github.com/repos/redis/jedis";

        // Mock cached labels
        List<Label> cachedLabels = List.of(
                Label.builder().name("bug").description("Something isn't working").build(),
                Label.builder().name("redis-cluster").description("Related to Redis cluster").build()
        );
        String cachedLabelsJson = objectMapper.writeValueAsString(cachedLabels);

        // Mock cached issues by label
        IssueContext bugIssue = IssueContext.builder()
                .title("Connection pool exhausted")
                .body("Pool is running out of connections")
                .build();
        IssueContext clusterIssue = IssueContext.builder()
                .title("Cluster failover issue")
                .body("Cluster is not failing over properly")
                .build();
        String bugIssueJson = objectMapper.writeValueAsString(bugIssue);
        String clusterIssueJson = objectMapper.writeValueAsString(clusterIssue);

        when(jedis.get("repo:redis/jedis:labels")).thenReturn(cachedLabelsJson);
        when(jedis.get("repo:redis/jedis:label:bug:issue")).thenReturn(bugIssueJson);
        when(jedis.get("repo:redis/jedis:label:redis-cluster:issue")).thenReturn(clusterIssueJson);
        when(gitHubService.formatLabelsForPrompt(cachedLabels)).thenReturn("Available labels:\n- bug: Something isn't working\n- redis-cluster: Related to Redis cluster\n");
        when(liteLLMClient.callLLM(anyString())).thenReturn("bug, redis-cluster");

        // When
        List<String> result = labelingService.generateLabels(issue, repositoryUrl);

        // Then
        assertThat(result).containsExactly("bug", "redis-cluster");
        verify(jedis).get("repo:redis/jedis:labels");
        verify(jedis).get("repo:redis/jedis:label:bug:issue");
        verify(jedis).get("repo:redis/jedis:label:redis-cluster:issue");
        verify(gitHubService, never()).fetchRepositoryLabels(anyString());
        verify(gitHubService, never()).fetchIssueByLabel(anyString(), anyString());
    }

    @Test
    void testGenerateLabelsWithCacheMiss() throws Exception {
        // Given
        GitHubIssue issue = GitHubIssue.builder()
                .title("Performance issue")
                .body("Slow response times")
                .build();

        String repositoryUrl = "https://api.github.com/repos/redis/lettuce";

        List<Label> fetchedLabels = List.of(
                Label.builder().name("performance").description("Performance related").build()
        );

        IssueContext performanceIssue = IssueContext.builder()
                .title("Slow queries")
                .body("Database queries are slow")
                .build();

        when(jedis.get("repo:redis/lettuce:labels")).thenReturn(null);
        when(jedis.get("repo:redis/lettuce:label:performance:issue")).thenReturn(null);
        when(gitHubService.fetchRepositoryLabels(repositoryUrl)).thenReturn(fetchedLabels);
        when(gitHubService.fetchIssueByLabel(repositoryUrl, "performance")).thenReturn(performanceIssue);
        when(gitHubService.formatLabelsForPrompt(fetchedLabels)).thenReturn("Available labels:\n- performance: Performance related\n");
        when(liteLLMClient.callLLM(anyString())).thenReturn("performance");

        // When
        List<String> result = labelingService.generateLabels(issue, repositoryUrl);

        // Then
        assertThat(result).containsExactly("performance");
        verify(gitHubService).fetchRepositoryLabels(repositoryUrl);
        verify(gitHubService).fetchIssueByLabel(repositoryUrl, "performance");
        verify(jedis).setex(eq("repo:redis/lettuce:labels"), eq(3600), anyString());
        verify(jedis).setex(eq("repo:redis/lettuce:label:performance:issue"), eq(1800), anyString()); // Issues have shorter TTL
    }

    @Test
    void testExtractRepoNameFromUrl() {
        // This tests the private method indirectly through generateLabels
        GitHubIssue issue = GitHubIssue.builder()
                .title("Test issue")
                .body("Test body")
                .build();

        String repositoryUrl = "https://api.github.com/repos/owner/repo-name";

        when(jedis.get("repo:owner/repo-name:labels")).thenReturn(null);
        when(gitHubService.fetchRepositoryLabels(repositoryUrl)).thenReturn(List.of());
        when(liteLLMClient.callLLM(anyString())).thenReturn("bug");

        // When
        labelingService.generateLabels(issue, repositoryUrl);

        // Then
        verify(jedis).get("repo:owner/repo-name:labels");
        // No labels means no issue fetching by label
    }
}
