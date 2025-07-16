package com.redis.triage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.triage.model.SimilarIssue;
import com.redis.triage.model.webhook.GitHubIssue;
import com.redis.triage.model.feign.Label;
import com.redis.triage.model.IssueContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
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
    private final SemanticSearchService semanticSearchService;
    private final JedisPooled jedis;
    private final ObjectMapper objectMapper;

    private static final int CACHE_TTL_SECONDS = 3600; // 1 hour cache TTL
    private static final int ISSUES_CACHE_TTL_SECONDS = 1800; // 30 minutes cache TTL for issues (more dynamic)

    // High confidence threshold for semantic matching (92% similarity)
    private static final double SIMILARITY_THRESHOLD = 0.92;

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
     * First checks semantic cache for high-confidence matches, falls back to LLM if needed
     *
     * @param issue The GitHub issue
     * @param repositoryUrl The repository URL to fetch labels from
     * @return List of suggested labels
     */
    public List<String> generateLabels(GitHubIssue issue, String repositoryUrl) {
        log.info("Generating labels for issue: {}", issue.getTitle());

        try {
            // Step 1: Try semantic cache first - build input text from issue
            String inputText = buildInputText(issue);
            log.debug("Built input text for semantic search: {}", inputText);

            Optional<SimilarIssue> match = semanticSearchService.findHighConfidenceMatch(inputText, SIMILARITY_THRESHOLD);

            if (match.isPresent()) {
                // CACHE_HIT: Use labels from similar issue
                SimilarIssue similarIssue = match.get();
                List<String> cachedLabels = extractLabelsFromSimilarIssue(similarIssue);

                log.info("CACHE_HIT: Reusing labels from issue {} (similarity: {:.2f})",
                        similarIssue.getIssue().getId(), similarIssue.getSimilarityScore());
                log.info("Cached labels for issue '{}': {}", issue.getTitle(), cachedLabels);

                return cachedLabels;
            } else {
                // CACHE_MISS: Fall back to LLM labeling
                log.info("CACHE_MISS: No high-similarity match found, using LLM");
                return generateLabelsWithLLM(issue, repositoryUrl);
            }

        } catch (Exception e) {
            log.error("Error in semantic cache check for issue '{}', falling back to LLM: {}",
                    issue.getTitle(), e.getMessage(), e);
            // Fall back to LLM on any error
            return generateLabelsWithLLM(issue, repositoryUrl);
        }
    }

    /**
     * Generates labels using LLM with repository context (original implementation)
     *
     * @param issue The GitHub issue
     * @param repositoryUrl The repository URL to fetch labels from
     * @return List of suggested labels
     */
    private List<String> generateLabelsWithLLM(GitHubIssue issue, String repositoryUrl) {
        log.debug("Generating labels using LLM for issue: {}", issue.getTitle());

        try {
            // Extract repository name from URL for caching
            String repoName = extractRepoNameFromUrl(repositoryUrl);

            // Fetch repository labels with caching
            List<Label> repositoryLabels = List.of();
            if (repositoryUrl != null && !repositoryUrl.isEmpty()) {
                repositoryLabels = getRepositoryLabels(repositoryUrl, repoName);
                log.debug("Got {} repository labels", repositoryLabels.size());
            }

            // Fetch repository issues by labels with caching
            Map<String, IssueContext> repositoryIssuesByLabel = new HashMap<>();
            if (repositoryUrl != null && !repositoryUrl.isEmpty() && !repositoryLabels.isEmpty()) {
                repositoryIssuesByLabel = getRepositoryIssuesByLabels(repositoryUrl, repoName, repositoryLabels);
                log.debug("Got {} repository issues by labels", repositoryIssuesByLabel.size());
            }

            // Build the prompt with repository context (labels and issues)
            String prompt = buildPrompt(issue, repositoryLabels, repositoryIssuesByLabel);
            log.debug("Built prompt for issue '{}': {}", issue.getTitle(), prompt);

            // Call LiteLLM to get label suggestions
            String response = liteLLMClient.callLLM(prompt);
            log.debug("Received response from LiteLLM: {}", response);

            // Parse the response into a list of labels
            List<String> labels = parseLabelsFromResponse(response);
            log.info("LLM generated {} labels for issue '{}': {}", labels.size(), issue.getTitle(), labels);

            return labels;

        } catch (Exception e) {
            log.error("Error generating labels with LLM for issue '{}': {}", issue.getTitle(), e.getMessage(), e);
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
     * Gets repository issues by labels with Redis caching
     * Fetches 1 issue per label and stores only title and body
     *
     * @param repositoryUrl The repository URL
     * @param repoName The repository name for cache key
     * @param labels The labels to fetch issues for
     * @return Map of label name to issue context (title and body)
     */
    private Map<String, IssueContext> getRepositoryIssuesByLabels(String repositoryUrl, String repoName, List<Label> labels) {
        Map<String, IssueContext> issuesByLabel = new HashMap<>();

        for (Label label : labels) {
            String cacheKey = "repo:" + repoName + ":label:" + label.getName() + ":issue";

            try {
                // Check if issue for this label is in Redis cache
                String cachedIssue = jedis.get(cacheKey);
                if (cachedIssue != null && !cachedIssue.isEmpty()) {
                    log.debug("Found cached issue for label '{}' in repository: {}", label.getName(), repoName);
                    IssueContext issueContext = objectMapper.readValue(cachedIssue, IssueContext.class);
                    issuesByLabel.put(label.getName(), issueContext);
                    continue;
                }

                // Cache miss - fetch from GitHub API
                log.debug("Cache miss for label '{}', fetching from GitHub API: {}", label.getName(), repoName);
                IssueContext issueContext = gitHubService.fetchIssueByLabel(repositoryUrl, label.getName());

                if (issueContext != null) {
                    // Store in Redis cache with shorter TTL since issues change more frequently
                    String issueJson = objectMapper.writeValueAsString(issueContext);
                    jedis.setex(cacheKey, ISSUES_CACHE_TTL_SECONDS, issueJson);
                    log.debug("Cached issue for label '{}' in repository: {} (TTL: {} seconds)",
                        label.getName(), repoName, ISSUES_CACHE_TTL_SECONDS);
                    issuesByLabel.put(label.getName(), issueContext);
                } else {
                    log.debug("No issue found for label '{}' in repository: {}", label.getName(), repoName);
                }

            } catch (JsonProcessingException e) {
                log.error("Error processing JSON for issue with label '{}': {}", label.getName(), e.getMessage(), e);
                // Try to fetch directly from API on JSON error
                try {
                    IssueContext issueContext = gitHubService.fetchIssueByLabel(repositoryUrl, label.getName());
                    if (issueContext != null) {
                        issuesByLabel.put(label.getName(), issueContext);
                    }
                } catch (Exception fallbackException) {
                    log.error("Fallback fetch also failed for label '{}': {}", label.getName(), fallbackException.getMessage());
                }
            } catch (Exception e) {
                log.error("Error getting issue for label '{}': {}", label.getName(), e.getMessage(), e);
            }
        }

        log.info("Successfully fetched {} issues by labels for repository: {}", issuesByLabel.size(), repoName);
        return issuesByLabel;
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
        return buildPrompt(issue, List.of(), new HashMap<>());
    }

    /**
     * Builds the prompt for the LiteLLM API with repository labels context (backward compatibility)
     *
     * @param issue The GitHub issue
     * @param repositoryLabels The labels available in the repository
     * @return The formatted prompt string
     */
    private String buildPrompt(GitHubIssue issue, List<Label> repositoryLabels) {
        return buildPrompt(issue, repositoryLabels, new HashMap<>());
    }

    /**
     * Builds the prompt for the LiteLLM API with repository labels and issues context
     *
     * @param issue The GitHub issue
     * @param repositoryLabels The labels available in the repository
     * @param repositoryIssuesByLabel Map of label names to issue contexts
     * @return The formatted prompt string
     */
    private String buildPrompt(GitHubIssue issue, List<Label> repositoryLabels, Map<String, IssueContext> repositoryIssuesByLabel) {
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
        if (repositoryIssuesByLabel != null && !repositoryIssuesByLabel.isEmpty()) {
            promptBuilder.append(formatIssuesByLabelForPrompt(repositoryIssuesByLabel));
            promptBuilder.append("\nConsider these example issues for each label when determining appropriate labels for the current issue.\n\n");
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
     * Formats issues by label into a readable text format for LLM prompts
     * Uses only title and body for each issue
     *
     * @param issuesByLabel Map of label names to issue contexts
     * @return Formatted issues text
     */
    private String formatIssuesByLabelForPrompt(Map<String, IssueContext> issuesByLabel) {
        if (issuesByLabel == null || issuesByLabel.isEmpty()) {
            return "No example issues available for labels in this repository.";
        }

        StringBuilder issuesText = new StringBuilder();
        issuesText.append("Example issues for each label (").append(issuesByLabel.size()).append(" labels with examples):\n");

        for (Map.Entry<String, IssueContext> entry : issuesByLabel.entrySet()) {
            String labelName = entry.getKey();
            IssueContext issueContext = entry.getValue();

            issuesText.append("- Label '").append(labelName).append("': ");
            issuesText.append(issueContext.getTitle() != null ? issueContext.getTitle() : "No title");

            if (issueContext.getBody() != null && !issueContext.getBody().isEmpty()) {
                issuesText.append(" - ").append(issueContext.getBody());
            }
            issuesText.append("\n");
        }

        return issuesText.toString();
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

    /**
     * Builds input text from issue title and body for semantic search
     *
     * @param issue The GitHub issue
     * @return Formatted input text
     */
    private String buildInputText(GitHubIssue issue) {
        String title = issue.getTitle() != null ? issue.getTitle() : "";
        String body = issue.getBody() != null ? issue.getBody() : "";

        return String.format("Title: %s\nBody: %s", title, body);
    }

    /**
     * Extracts label names from a similar issue
     *
     * @param similarIssue The similar issue with labels
     * @return List of label names
     */
    private List<String> extractLabelsFromSimilarIssue(SimilarIssue similarIssue) {
        if (similarIssue.getIssue() == null || similarIssue.getIssue().getLabels() == null) {
            return List.of();
        }

        return similarIssue.getIssue().getLabels().stream()
                .map(label -> label.getName())
                .filter(name -> name != null && !name.trim().isEmpty())
                .toList();
    }
}
