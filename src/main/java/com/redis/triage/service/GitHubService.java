package com.redis.triage.service;

import com.redis.triage.model.GitHubIssue;
import com.redis.triage.model.Label;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
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

    /**
     * Fetches issues from a GitHub repository with pagination support
     *
     * @param repositoryUrl The repository URL from the webhook payload
     * @return List of issues from the repository (limited to recent issues for LLM context)
     */
    public List<GitHubIssue> fetchRepositoryIssues(String repositoryUrl) {
        if (repositoryUrl == null || repositoryUrl.isEmpty()) {
            log.warn("Repository URL is null or empty, cannot fetch issues");
            return List.of();
        }

        try {
            List<GitHubIssue> allIssues = new ArrayList<>();
            int page = 1;
            int maxPages = 5; // Limit to first 5 pages (500 issues max) for LLM context
            int perPage = 100;

            while (page <= maxPages) {
                // Construct the paginated issues URL
                String issuesUrl = constructIssuesUrl(repositoryUrl, page, perPage);
                log.debug("Fetching issues from page {}: {}", page, issuesUrl);

                // Make the API call to fetch issues for this page
                Mono<List<GitHubIssue>> responseMono = githubWebClient
                    .get()
                    .uri(issuesUrl)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<GitHubIssue>>() {});

                List<GitHubIssue> pageIssues = responseMono.block();

                if (pageIssues == null || pageIssues.isEmpty()) {
                    log.debug("No more issues found on page {}, stopping pagination", page);
                    break;
                }

                allIssues.addAll(pageIssues);
                log.debug("Fetched {} issues from page {}, total so far: {}",
                    pageIssues.size(), page, allIssues.size());

                // If we got fewer issues than requested per page, we've reached the end
                if (pageIssues.size() < perPage) {
                    log.debug("Received fewer issues than requested per page, reached end of results");
                    break;
                }

                page++;
            }

            log.info("Successfully fetched {} issues from repository across {} pages",
                allIssues.size(), page - 1);
            log.debug("Fetched issues: {}", allIssues.stream().map(GitHubIssue::getTitle).toList());

            return allIssues;

        } catch (WebClientResponseException e) {
            log.error("GitHub API error when fetching issues: {} - {}", e.getStatusCode(), e.getMessage());
            return List.of();
        } catch (Exception e) {
            log.error("Error fetching repository issues: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Constructs the issues URL from a GitHub repository URL with pagination support
     *
     * @param repositoryUrl The GitHub repository URL
     * @param page The page number (1-based)
     * @param perPage The number of items per page
     * @return The issues endpoint URL with pagination parameters
     */
    private String constructIssuesUrl(String repositoryUrl, int page, int perPage) {
        if (repositoryUrl == null || repositoryUrl.isEmpty()) {
            return "";
        }

        // Handle different URL types:
        // Repository URL format: https://api.github.com/repos/owner/repo
        if (repositoryUrl.startsWith("https://api.github.com")) {
            // Remove the base URL to get the path
            String path = repositoryUrl.replace("https://api.github.com", "");
            return path + "/issues?state=all&sort=updated&direction=desc&per_page=" + perPage + "&page=" + page;
        } else {
            // Fallback: assume it's already a path
            return repositoryUrl + "/issues?state=all&sort=updated&direction=desc&per_page=" + perPage + "&page=" + page;
        }
    }

    /**
     * Constructs the issues URL from a GitHub repository URL (backward compatibility)
     *
     * @param repositoryUrl The GitHub repository URL
     * @return The issues endpoint URL
     */
    private String constructIssuesUrl(String repositoryUrl) {
        return constructIssuesUrl(repositoryUrl, 1, 100);
    }

    /**
     * Formats issues into a readable text format for LLM prompts
     *
     * @param issues List of issues to format
     * @return Formatted issues text
     */
    public String formatIssuesForPrompt(List<GitHubIssue> issues) {
        if (issues == null || issues.isEmpty()) {
            return "No recent issues available in this repository.";
        }

        StringBuilder issuesText = new StringBuilder();
        issuesText.append("Recent issues in this repository (").append(issues.size()).append(" total):\n");

        // Limit to first 15 issues to provide good context without overwhelming the LLM
        int limit = Math.min(issues.size(), 15);
        for (int i = 0; i < limit; i++) {
            GitHubIssue issue = issues.get(i);
            issuesText.append("- ").append(issue.getTitle());

            // Add state information
            if (issue.getState() != null) {
                issuesText.append(" [").append(issue.getState().toUpperCase()).append("]");
            }

            // Add existing labels if available
            if (issue.getLabels() != null && !issue.getLabels().isEmpty()) {
                issuesText.append(" (labels: ");
                issuesText.append(issue.getLabels().stream()
                    .map(label -> label.getName())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse(""));
                issuesText.append(")");
            }

            // Add full body for context
            if (issue.getBody() != null && !issue.getBody().isEmpty()) {
                issuesText.append(" - ").append(issue.getBody());
            }
            issuesText.append("\n");
        }

        if (issues.size() > limit) {
            issuesText.append("... and ").append(issues.size() - limit).append(" more issues\n");
        }

        return issuesText.toString();
    }
}
