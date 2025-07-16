package com.redis.triage.service;

import com.redis.triage.model.SimilarIssue;
import com.redis.triage.model.webhook.GitHubIssue;
import com.redis.triage.model.webhook.GitHubLabel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test for LabelingService semantic caching functionality
 * Tests the cache hit/miss logic without requiring real Redis or LLM calls
 */
@ExtendWith(MockitoExtension.class)
class LabelingServiceCacheTest {

    @Mock
    private SemanticSearchService semanticSearchService;

    @Mock
    private LiteLLMClient liteLLMClient;

    @Mock
    private GitHubService gitHubService;

    @InjectMocks
    private LabelingService labelingService;

    private GitHubIssue testIssue;
    private List<String> expectedLabels;

    @BeforeEach
    void setUp() {
        testIssue = GitHubIssue.builder()
                .id(1L)
                .title("Test issue")
                .body("Test issue body")
                .repositoryUrl("https://api.github.com/repos/test/repo")
                .build();

        expectedLabels = List.of("bug", "enhancement");
    }

    @Test
    void shouldReturnCachedLabelsOnCacheHit() {
        // Given: Semantic search returns a high-confidence match
        GitHubIssue similarIssue = GitHubIssue.builder()
                .id(100L)
                .title("Similar issue")
                .body("Similar issue body")
                .labels(List.of(
                        GitHubLabel.builder().name("bug").build(),
                        GitHubLabel.builder().name("enhancement").build()
                ))
                .build();

        SimilarIssue match = SimilarIssue.builder()
                .issue(similarIssue)
                .similarityScore(0.95)
                .build();

        when(semanticSearchService.findHighConfidenceMatch(anyString(), eq(0.92)))
                .thenReturn(Optional.of(match));

        // When: Generate labels
        List<String> result = labelingService.generateLabels(testIssue, testIssue.getRepositoryUrl());

        // Then: Should return cached labels without calling LLM
        assertThat(result).containsExactlyInAnyOrder("bug", "enhancement");
        
        // Verify semantic search was called
        verify(semanticSearchService).findHighConfidenceMatch(anyString(), eq(0.92));
        
        // Verify LLM was NOT called (cache hit)
        verifyNoInteractions(liteLLMClient);
        verifyNoInteractions(gitHubService);
    }

    @Test
    void shouldUseLLMOnCacheMiss() {
        // Given: Semantic search returns no match
        when(semanticSearchService.findHighConfidenceMatch(anyString(), eq(0.92)))
                .thenReturn(Optional.empty());

        // Mock LLM response
        when(liteLLMClient.callLLM(anyString())).thenReturn("bug, enhancement");

        // When: Generate labels
        List<String> result = labelingService.generateLabels(testIssue, testIssue.getRepositoryUrl());

        // Then: Should use LLM and return generated labels
        assertThat(result).containsExactlyInAnyOrder("bug", "enhancement");
        
        // Verify semantic search was called
        verify(semanticSearchService).findHighConfidenceMatch(anyString(), eq(0.92));
        
        // Verify LLM was called (cache miss)
        verify(liteLLMClient).callLLM(anyString());
    }

    @Test
    void shouldHandleEmptyLabelsInCachedIssue() {
        // Given: Semantic search returns match with no labels
        GitHubIssue issueWithNoLabels = GitHubIssue.builder()
                .id(100L)
                .title("Issue without labels")
                .labels(List.of()) // Empty labels
                .build();

        SimilarIssue match = SimilarIssue.builder()
                .issue(issueWithNoLabels)
                .similarityScore(0.95)
                .build();

        when(semanticSearchService.findHighConfidenceMatch(anyString(), eq(0.92)))
                .thenReturn(Optional.of(match));

        // When: Generate labels
        List<String> result = labelingService.generateLabels(testIssue, testIssue.getRepositoryUrl());

        // Then: Should return empty list from cache
        assertThat(result).isEmpty();
        
        // Verify LLM was NOT called
        verifyNoInteractions(liteLLMClient);
    }

    @Test
    void shouldHandleNullLabelsInCachedIssue() {
        // Given: Semantic search returns match with null labels
        GitHubIssue issueWithNullLabels = GitHubIssue.builder()
                .id(100L)
                .title("Issue with null labels")
                .labels(null) // Null labels
                .build();

        SimilarIssue match = SimilarIssue.builder()
                .issue(issueWithNullLabels)
                .similarityScore(0.95)
                .build();

        when(semanticSearchService.findHighConfidenceMatch(anyString(), eq(0.92)))
                .thenReturn(Optional.of(match));

        // When: Generate labels
        List<String> result = labelingService.generateLabels(testIssue, testIssue.getRepositoryUrl());

        // Then: Should return empty list from cache
        assertThat(result).isEmpty();
        
        // Verify LLM was NOT called
        verifyNoInteractions(liteLLMClient);
    }

    @Test
    void shouldFallbackToLLMOnSemanticSearchException() {
        // Given: Semantic search throws exception
        when(semanticSearchService.findHighConfidenceMatch(anyString(), eq(0.92)))
                .thenThrow(new RuntimeException("Redis connection failed"));

        // Mock LLM response for fallback
        when(liteLLMClient.callLLM(anyString())).thenReturn("fallback, labels");

        // When: Generate labels
        List<String> result = labelingService.generateLabels(testIssue, testIssue.getRepositoryUrl());

        // Then: Should fallback to LLM
        assertThat(result).containsExactlyInAnyOrder("fallback", "labels");
        
        // Verify LLM was called as fallback
        verify(liteLLMClient).callLLM(anyString());
    }

    @Test
    void shouldFilterOutEmptyLabelNames() {
        // Given: Semantic search returns match with some empty/null label names
        List<GitHubLabel> labelsWithEmpties = List.of(
                GitHubLabel.builder().name("bug").build(),
                GitHubLabel.builder().name("").build(), // Empty name
                GitHubLabel.builder().name("enhancement").build(),
                GitHubLabel.builder().name(null).build(), // Null name
                GitHubLabel.builder().name("  ").build() // Whitespace only
        );

        GitHubIssue issueWithMixedLabels = GitHubIssue.builder()
                .id(100L)
                .labels(labelsWithEmpties)
                .build();

        SimilarIssue match = SimilarIssue.builder()
                .issue(issueWithMixedLabels)
                .similarityScore(0.95)
                .build();

        when(semanticSearchService.findHighConfidenceMatch(anyString(), eq(0.92)))
                .thenReturn(Optional.of(match));

        // When: Generate labels
        List<String> result = labelingService.generateLabels(testIssue, testIssue.getRepositoryUrl());

        // Then: Should only return valid label names
        assertThat(result).containsExactlyInAnyOrder("bug", "enhancement");
    }
}
