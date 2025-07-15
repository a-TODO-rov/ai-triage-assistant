package com.redis.triage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.triage.model.GitHubIssue;
import com.redis.triage.model.Label;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for generating labels for GitHub issues using AI
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LabelingService {

    private final LiteLLMClient liteLLMClient;
    private final GitHubService gitHubService;
    private final JedisPooled jedis;
    private final ObjectMapper objectMapper;

    private static final int CACHE_TTL_SECONDS = 3600; // 1 hour cache TTL
    private static final int ISSUES_CACHE_TTL_SECONDS = 1800; // 30 minutes cache TTL for issues (more dynamic)

    /**
     * Generates appropriate labels for a GitHub issue
     *
     * @param issue The GitHub issue
     * @return List of suggested labels
     */
    public List<String> generateLabels(GitHubIssue issue) {
        return generateLabels(issue, null);
    }

    /**
     * Generates appropriate labels for a GitHub issue with repository context
     *
     * @param issue The GitHub issue
     * @param repositoryUrl The repository URL to fetch labels from
     * @return List of suggested labels
     */
    public List<String> generateLabels(GitHubIssue issue, String repositoryUrl) {
        log.info("Generating labels for issue: {}", issue.getTitle());

        try {
            // Extract repository name from URL for caching
            String repoName = extractRepoNameFromUrl(repositoryUrl);

            // Fetch repository labels with caching
            List<Label> repositoryLabels = List.of();
            if (repositoryUrl != null && !repositoryUrl.isEmpty()) {
                repositoryLabels = getRepositoryLabels(repositoryUrl, repoName);
                log.debug("Got {} repository labels", repositoryLabels.size());
            }

            // Fetch repository issues with caching
            List<GitHubIssue> repositoryIssues = List.of();
            if (repositoryUrl != null && !repositoryUrl.isEmpty()) {
                repositoryIssues = getRepositoryIssues(repositoryUrl, repoName);
                log.debug("Got {} repository issues", repositoryIssues.size());
            }

            // Build the prompt with repository context (labels and issues)
            String prompt = buildPrompt(issue, repositoryLabels, repositoryIssues);
            log.debug("Built prompt for issue '{}': {}", issue.getTitle(), prompt);

            // Call LiteLLM to get label suggestions
            String response = liteLLMClient.callLLM(prompt);
            log.debug("Received response from LiteLLM: {}", response);

            // Parse the response into a list of labels
            List<String> labels = parseLabelsFromResponse(response);
            log.info("Generated {} labels for issue '{}': {}", labels.size(), issue.getTitle(), labels);

            return labels;

        } catch (Exception e) {
            log.error("Error generating labels for issue '{}': {}", issue.getTitle(), e.getMessage(), e);
            return List.of(); // Return empty list on error
        }
    }

    /**
     * Gets repository labels with Redis caching
     *
     * @param repositoryUrl The repository URL
     * @param repoName The repository name for cache key
     * @return List of labels
     */
    private List<Label> getRepositoryLabels(String repositoryUrl, String repoName) {
        String cacheKey = "repo:" + repoName + ":labels";

        try {
            // Check if labels are in Redis cache
            String cachedLabels = jedis.get(cacheKey);
            if (cachedLabels != null && !cachedLabels.isEmpty()) {
                log.info("Found cached labels for repository: {}", repoName);
                return objectMapper.readValue(cachedLabels, new TypeReference<List<Label>>() {});
            }

            // Cache miss - fetch from GitHub API
            log.info("Cache miss for labels, fetching from GitHub API: {}", repoName);
            List<Label> labels = gitHubService.fetchRepositoryLabels(repositoryUrl);

            // Store in Redis cache
            if (!labels.isEmpty()) {
                String labelsJson = objectMapper.writeValueAsString(labels);
                jedis.setex(cacheKey, CACHE_TTL_SECONDS, labelsJson);
                log.info("Cached {} labels for repository: {}", labels.size(), repoName);
            }

            return labels;
        } catch (JsonProcessingException e) {
            log.error("Error processing JSON for repository labels: {}", e.getMessage(), e);
            // Fallback to direct API call on error
            return gitHubService.fetchRepositoryLabels(repositoryUrl);
        } catch (Exception e) {
            log.error("Error getting repository labels: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Gets repository issues with Redis caching
     *
     * @param repositoryUrl The repository URL
     * @param repoName The repository name for cache key
     * @return List of issues
     */
    private List<GitHubIssue> getRepositoryIssues(String repositoryUrl, String repoName) {
        String cacheKey = "repo:" + repoName + ":issues";

        try {
            // Check if issues are in Redis cache
            String cachedIssues = jedis.get(cacheKey);
            if (cachedIssues != null && !cachedIssues.isEmpty()) {
                log.info("Found cached issues for repository: {}", repoName);
                return objectMapper.readValue(cachedIssues, new TypeReference<List<GitHubIssue>>() {});
            }

            // Cache miss - fetch from GitHub API with pagination
            log.info("Cache miss for issues, fetching from GitHub API with pagination: {}", repoName);
            List<GitHubIssue> issues = gitHubService.fetchRepositoryIssues(repositoryUrl);

            // Store in Redis cache with shorter TTL since issues change more frequently
            if (!issues.isEmpty()) {
                String issuesJson = objectMapper.writeValueAsString(issues);
                jedis.setex(cacheKey, ISSUES_CACHE_TTL_SECONDS, issuesJson);
                log.info("Cached {} issues for repository: {} (TTL: {} seconds)",
                    issues.size(), repoName, ISSUES_CACHE_TTL_SECONDS);
            } else {
                log.warn("No issues fetched from GitHub API for repository: {}", repoName);
            }

            return issues;
        } catch (JsonProcessingException e) {
            log.error("Error processing JSON for repository issues: {}", e.getMessage(), e);
            // Fallback to direct API call on error
            return gitHubService.fetchRepositoryIssues(repositoryUrl);
        } catch (Exception e) {
            log.error("Error getting repository issues: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Extracts repository name from URL for cache key
     *
     * @param repositoryUrl The repository URL
     * @return Repository name in format "owner/repo"
     */
    private String extractRepoNameFromUrl(String repositoryUrl) {
        if (repositoryUrl == null || repositoryUrl.isEmpty()) {
            return "unknown";
        }

        try {
            // Handle different URL formats
            // Format: https://api.github.com/repos/owner/repo
            if (repositoryUrl.contains("/repos/")) {
                String[] parts = repositoryUrl.split("/repos/");
                if (parts.length > 1) {
                    return parts[1];
                }
            }

            // Fallback: use URL hash
            return String.valueOf(Math.abs(repositoryUrl.hashCode()));
        } catch (Exception e) {
            log.error("Error extracting repo name from URL: {}", e.getMessage(), e);
            return String.valueOf(Math.abs(repositoryUrl.hashCode()));
        }
    }

    /**
     * Builds the prompt for the LiteLLM API (backward compatibility)
     *
     * @param issue The GitHub issue
     * @return The formatted prompt string
     */
    private String buildPrompt(GitHubIssue issue) {
        return buildPrompt(issue, List.of(), List.of());
    }

    /**
     * Builds the prompt for the LiteLLM API with repository labels context (backward compatibility)
     *
     * @param issue The GitHub issue
     * @param repositoryLabels The labels available in the repository
     * @return The formatted prompt string
     */
    private String buildPrompt(GitHubIssue issue, List<Label> repositoryLabels) {
        return buildPrompt(issue, repositoryLabels, List.of());
    }

    /**
     * Builds the prompt for the LiteLLM API with repository labels and issues context
     *
     * @param issue The GitHub issue
     * @param repositoryLabels The labels available in the repository
     * @param repositoryIssues The issues in the repository
     * @return The formatted prompt string
     */
    private String buildPrompt(GitHubIssue issue, List<Label> repositoryLabels, List<GitHubIssue> repositoryIssues) {
        StringBuilder promptBuilder = new StringBuilder();

        promptBuilder.append("You are an AI triage assistant. Read the following GitHub issue and return relevant labels.\n\n");

        // Add repository labels context if available
        if (repositoryLabels != null && !repositoryLabels.isEmpty()) {
            promptBuilder.append(gitHubService.formatLabelsForPrompt(repositoryLabels));
            promptBuilder.append("\nPlease select only from the labels listed above that are relevant to this issue.\n\n");
        } else {
            // Fallback to default labels if no repository labels available
            promptBuilder.append("Available labels: ['bug', 'feature', 'question', 'documentation', 'redis-cluster', 'jedis', 'lettuce', 'performance', 'regression']\n\n");
        }

        // Add repository issues context if available
        if (repositoryIssues != null && !repositoryIssues.isEmpty()) {
            promptBuilder.append(gitHubService.formatIssuesForPrompt(repositoryIssues));
            promptBuilder.append("\nConsider these recent issues when determining appropriate labels for the current issue.\n\n");
        }

        promptBuilder.append("Issue to label:\n");
        promptBuilder.append("---\n");
        promptBuilder.append("Title: ").append(issue.getTitle() != null ? issue.getTitle() : "").append("\n");
        promptBuilder.append("Body: ").append(issue.getBody() != null ? issue.getBody() : "").append("\n");
        promptBuilder.append("---\n");
        promptBuilder.append("Return only a list of relevant labels as a comma-separated string.");

        return promptBuilder.toString();
    }

    /**
     * Parses the LiteLLM response into a list of labels
     *
     * @param response The raw response from LiteLLM
     * @return List of parsed and trimmed labels
     */
    private List<String> parseLabelsFromResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            log.warn("Received empty or null response from LiteLLM");
            return List.of();
        }

        // Split by comma, trim whitespace, and filter out empty strings
        List<String> labels = Arrays.stream(response.split(","))
                .map(String::trim)
                .filter(label -> !label.isEmpty())
                .collect(Collectors.toList());

        log.debug("Parsed {} labels from response: {}", labels.size(), labels);
        return labels;
    }
}
