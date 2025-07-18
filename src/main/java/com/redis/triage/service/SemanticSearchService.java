package com.redis.triage.service;

import com.redis.triage.model.GitHubSimilarIssue;
import com.redis.triage.model.SimilarIssueResult;
import com.redis.triage.model.webhook.GitHubIssue;
import com.redis.triage.model.webhook.GitHubLabel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for performing semantic search on GitHub issues using vector embeddings
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SemanticSearchService {

    private final LiteLLMClient liteLLMClient;
    private final RedisVectorStoreService redisVectorStoreService;

    /**
     * Finds similar issues using semantic search based on vector embeddings
     *
     * @param issue The GitHub issue to find similar issues for
     * @param topK The number of top similar issues to return
     * @return List of similar issues with metadata
     */
    public List<GitHubSimilarIssue> findSimilarIssues(GitHubIssue issue, int topK) {
        log.info("Finding {} similar issues for: {}", topK, issue.getTitle());

        try {
            // Step 1: Generate embedding from issue title and body
            String searchText = buildSearchText(issue);
            log.debug("Generated search text: {}", searchText);

            float[] queryEmbedding = liteLLMClient.generateEmbedding(searchText);
            if (queryEmbedding.length == 0) {
                log.error("Failed to generate embedding for issue: {}", issue.getTitle());
                return List.of();
            }

            log.info("Generated embedding with {} dimensions", queryEmbedding.length);

            // Step 2: Search for similar issues using vector similarity
            List<SimilarIssueResult> similarIssues = redisVectorStoreService.searchSimilarIssues(queryEmbedding, topK);
            log.info("Found {} similar issue keys: {}", similarIssues.size(), similarIssues);

            // Step 3: Retrieve metadata for each similar issue
            List<GitHubSimilarIssue> gitHubSimilarIssues = new ArrayList<>();
            for (SimilarIssueResult result : similarIssues) {
                GitHubIssue similarIssue = buildSimilarIssue(result.redisKey());
                if (similarIssue != null) {
                    gitHubSimilarIssues.add(new GitHubSimilarIssue(similarIssue, result.distance(), result.similarityScore()));
                }
            }

            log.info("Successfully found {} similar issues with metadata", gitHubSimilarIssues.size());
            return gitHubSimilarIssues;

        } catch (Exception e) {
            log.error("Error finding similar issues for '{}': {}", issue.getTitle(), e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Atomically finds similar issues and stores the new issue for future searches
     * This ensures the new issue doesn't match itself and becomes available for future queries
     *
     * @param issue The GitHub issue to process
     * @param labels The generated labels for the issue
     * @param topK The number of top similar issues to return
     * @return List of similar issues found before storing the new one
     */
    public List<GitHubSimilarIssue> findSimilarIssuesAndStore(GitHubIssue issue, List<String> labels, int topK) {
        log.info("Finding {} similar issues and storing new issue: {}", topK, issue.getTitle());

        try {
            // Step 1: Generate embedding from issue title and body
            String searchText = buildSearchText(issue);
            log.debug("Generated search text: {}", searchText);

            float[] queryEmbedding = liteLLMClient.generateEmbedding(searchText);
            if (queryEmbedding.length == 0) {
                log.error("Failed to generate embedding for issue: {}", issue.getTitle());
                return List.of();
            }

            log.info("Generated embedding with {} dimensions", queryEmbedding.length);

            // Step 2: Search for similar issues BEFORE storing the new one
            List<SimilarIssueResult> similarIssueKeys = redisVectorStoreService.searchSimilarIssues(queryEmbedding, topK);
            log.info("Found {} similar issue keys: {}", similarIssueKeys.size(), similarIssueKeys);

            // Step 3: Retrieve metadata for each similar issue
            List<GitHubSimilarIssue> similarIssues = new ArrayList<>();
            for (SimilarIssueResult similarIssueResult : similarIssueKeys) {
                GitHubIssue similarIssue = buildSimilarIssue(similarIssueResult.redisKey());
                if (similarIssue != null && !similarIssue.getId().equals(issue.getId())) {
                    similarIssues.add(new GitHubSimilarIssue(similarIssue, similarIssueResult.distance(), similarIssueResult.similarityScore()));
                }
            }
            similarIssues.sort(Comparator.comparingInt(GitHubSimilarIssue::similarityScore));

            // Step 4: Store the new issue in Redis for future similarity searches
            String issueId = extractIssueId(issue);
            redisVectorStoreService.storeEmbedding(
                issueId,
                queryEmbedding,
                issue.getTitle(),
                issue.getBody(),
                labels,
                issue.getHtmlUrl()
            );
            log.info("Successfully stored issue '{}' with ID '{}' in Redis", issue.getTitle(), issueId);

            log.info("Successfully found {} similar issues and stored new issue", similarIssues.size());
            return similarIssues;

        } catch (Exception e) {
            log.error("Error in findSimilarIssuesAndStore for '{}': {}", issue.getTitle(), e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Builds the search text from issue title and body
     *
     * @param issue The GitHub issue
     * @return Formatted search text
     */
    private String buildSearchText(GitHubIssue issue) {
        String title = issue.getTitle() != null ? issue.getTitle() : "";
        String body = issue.getBody() != null ? issue.getBody() : "";

        return String.format("Title: %s\nBody: %s", title, body);
    }

    /**
     * Builds a GitHubIssue from Redis issue key and metadata
     *
     * @param issueKey The Redis key for the issue (e.g., "issue:123")
     * @return GitHubIssue or null if metadata retrieval fails
     */
    private GitHubIssue buildSimilarIssue(String issueKey) {
        try {
            // Extract issue ID from the key (remove "issue:" prefix)
            String issueId = extractIssueId(issueKey);

            // Retrieve metadata from Redis
            Map<String, String> metadata = redisVectorStoreService.getIssueMetadata(issueKey);
            if (metadata.isEmpty()) {
                log.warn("No metadata found for issue key: {}", issueKey);
                return null;
            }

            // Extract title, body, and labels from metadata
            String title = metadata.getOrDefault("title", "Unknown Title");
            String body = metadata.getOrDefault("body", "");
            String labelsString = metadata.getOrDefault("labels", "");
            String url = metadata.getOrDefault("url", "");
            List<String> labels = parseLabels(labelsString);

            // Build a minimal GitHubIssue with available metadata
            return GitHubIssue.builder()
                    .id(Long.parseLong(issueId))
                    .title(title)
                    .body(body)
                    .url(url)
                    .labels(labels.stream()
                            .map(labelName -> GitHubLabel.builder()
                                    .name(labelName)
                                    .build())
                            .toList())
                    .build();

        } catch (Exception e) {
            log.error("Error building GitHubIssue from key '{}': {}", issueKey, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Extracts the issue ID from a Redis key
     *
     * @param issueKey The Redis key (e.g., "issue:123")
     * @return The issue ID (e.g., "123")
     */
    private String extractIssueId(String issueKey) {
        if (issueKey.startsWith("issue:")) {
            return issueKey.substring("issue:".length());
        }
        return issueKey;
    }

    /**
     * Parses a comma-separated labels string into a list
     *
     * @param labelsString Comma-separated labels string
     * @return List of labels
     */
    private List<String> parseLabels(String labelsString) {
        if (labelsString == null || labelsString.trim().isEmpty()) {
            return List.of();
        }

        return Arrays.stream(labelsString.split(","))
                .map(String::trim)
                .filter(label -> !label.isEmpty())
                .toList();
    }

    /**
     * Extracts the issue ID from the GitHub issue
     *
     * @param issue The GitHub issue
     * @return A unique issue ID
     */
    private String extractIssueId(GitHubIssue issue) {
        // Use the issue ID directly from the GitHub API
        if (issue.getId() != null) {
            return String.valueOf(issue.getId());
        }

        // Fallback to issue number if ID is not available
        if (issue.getNumber() != null) {
            return String.valueOf(issue.getNumber());
        }

        // Try to extract from HTML URL (e.g., https://github.com/owner/repo/issues/123)
        if (issue.getHtmlUrl() != null && issue.getHtmlUrl().contains("/issues/")) {
            String[] parts = issue.getHtmlUrl().split("/issues/");
            if (parts.length > 1) {
                return parts[1].split("[^0-9]")[0]; // Extract just the number
            }
        }

        // Fallback: generate based on title hash and timestamp
        String titleHash = String.valueOf(Math.abs(issue.getTitle().hashCode()));
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        return titleHash + "_" + timestamp;
    }
}
