package com.redis.triage.service;

import com.redis.triage.model.GitHubIssue;
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

    /**
     * Generates appropriate labels for a GitHub issue
     *
     * @param issue The GitHub issue
     * @return List of suggested labels
     */
    public List<String> generateLabels(GitHubIssue issue) {
        log.info("Generating labels for issue: {}", issue.getTitle());

        try {
            // Build the prompt
            String prompt = buildPrompt(issue);
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
     * Builds the prompt for the LiteLLM API
     *
     * @param issue The GitHub issue
     * @return The formatted prompt string
     */
    private String buildPrompt(GitHubIssue issue) {
        return String.format("""
            You are an AI triage assistant. Read the following GitHub issue and return relevant labels from this list:
            ['bug', 'feature', 'question', 'documentation', 'redis-cluster', 'jedis', 'lettuce', 'performance', 'regression']

            Issue:
            ---
            Title: %s
            Body: %s
            ---
            Return only a list of relevant labels as a comma-separated string.""",
            issue.getTitle() != null ? issue.getTitle() : "",
            issue.getBody() != null ? issue.getBody() : ""
        );
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
