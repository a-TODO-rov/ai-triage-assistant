package com.redis.triage.service;

import com.redis.triage.model.Label;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Service for interacting with GitHub API
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubService {

    @Qualifier("githubWebClient")
    private final WebClient githubWebClient;

    /**
     * Fetches labels from a GitHub repository
     *
     * @param repositoryUrl The repository URL from the webhook payload
     * @return List of labels from the repository
     */
    public List<Label> fetchRepositoryLabels(String repositoryUrl) {
        if (repositoryUrl == null || repositoryUrl.isEmpty()) {
            log.warn("Repository URL is null or empty, cannot fetch labels");
            return List.of();
        }

        try {
            // Extract the repository path from the URL and construct labels endpoint
            String labelsUrl = constructLabelsUrl(repositoryUrl);
            log.info("Fetching labels from: {}", labelsUrl);

            // Make the API call to fetch labels
            Mono<List<Label>> responseMono = githubWebClient
                .get()
                .uri(labelsUrl)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Label>>() {});

            List<Label> labels = responseMono.block();
            
            if (labels != null) {
                log.info("Successfully fetched {} labels from repository", labels.size());
                log.debug("Fetched labels: {}", labels.stream().map(Label::getName).toList());
                return labels;
            } else {
                log.warn("Received null response when fetching labels");
                return List.of();
            }

        } catch (WebClientResponseException e) {
            log.error("GitHub API error when fetching labels: {} - {}", e.getStatusCode(), e.getMessage());
            return List.of();
        } catch (Exception e) {
            log.error("Error fetching repository labels: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Constructs the labels URL from a GitHub URL (repository or issue)
     *
     * @param githubUrl The GitHub URL (repository or issue)
     * @return The labels endpoint URL
     */
    private String constructLabelsUrl(String githubUrl) {
        if (githubUrl == null || githubUrl.isEmpty()) {
            return "";
        }

        // Handle different URL types:
        // Repository URL format: https://api.github.com/repos/owner/repo
        // Issue URL format: https://api.github.com/repos/owner/repo/issues/123

        if (githubUrl.startsWith("https://api.github.com")) {
            // Remove the base URL to get the path
            String path = githubUrl.replace("https://api.github.com", "");

            // Check if it's an issue URL
            if (path.contains("/issues/")) {
                return path + "/labels";
            }
            // Repository URL
            else {
                return path + "/labels";
            }
        } else {
            // Fallback: assume it's already a path
            return githubUrl + "/labels";
        }
    }

    /**
     * Formats labels into a readable text format for LLM prompts
     *
     * @param labels List of labels to format
     * @return Formatted labels text
     */
    public String formatLabelsForPrompt(List<Label> labels) {
        if (labels == null || labels.isEmpty()) {
            return "No labels available in this repository.";
        }

        StringBuilder labelsText = new StringBuilder("Available labels in this repository:\n");
        for (Label label : labels) {
            labelsText.append("- ").append(label.getName());
            if (label.getDescription() != null && !label.getDescription().isEmpty()) {
                labelsText.append(": ").append(label.getDescription());
            }
            labelsText.append("\n");
        }

        return labelsText.toString();
    }
}
