package com.redis.triage.service;

import com.redis.triage.model.GitHubIssue;
import com.redis.triage.model.Label;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
            // Fetch repository labels if repository URL is provided
            List<Label> repositoryLabels = List.of();
            if (repositoryUrl != null && !repositoryUrl.isEmpty()) {
                repositoryLabels = gitHubService.fetchRepositoryLabels(repositoryUrl);
                log.debug("Fetched {} repository labels", repositoryLabels.size());
            }

            // Build the prompt with repository context
            String prompt = buildPrompt(issue, repositoryLabels);
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
     * Builds the prompt for the LiteLLM API (backward compatibility)
     *
     * @param issue The GitHub issue
     * @return The formatted prompt string
     */
    private String buildPrompt(GitHubIssue issue) {
        return buildPrompt(issue, List.of());
    }

    /**
     * Builds the prompt for the LiteLLM API with repository labels context
     *
     * @param issue The GitHub issue
     * @param repositoryLabels The labels available in the repository
     * @return The formatted prompt string
     */
    private String buildPrompt(GitHubIssue issue, List<Label> repositoryLabels) {
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

        promptBuilder.append("Issue:\n");
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
