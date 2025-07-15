package com.redis.triage.service;

import com.redis.triage.model.webhook.GitHubIssue;
import com.redis.triage.client.GitHubFeignClient;
import com.redis.triage.model.feign.Label;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for interacting with GitHub API
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubService {

    private final GitHubFeignClient gitHubFeignClient;

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
            // Extract owner and repo from the URL
            String[] ownerAndRepo = extractOwnerAndRepo(repositoryUrl);
            if (ownerAndRepo == null) {
                log.warn("Could not extract owner and repo from URL: {}", repositoryUrl);
                return List.of();
            }

            String owner = ownerAndRepo[0];
            String repo = ownerAndRepo[1];
            log.info("Fetching labels from repository: {}/{}", owner, repo);

            // Make the API call to fetch labels
            List<Label> labels = gitHubFeignClient.getRepositoryLabels(owner, repo);

            if (labels != null) {
                log.info("Successfully fetched {} labels from repository", labels.size());
                log.debug("Fetched labels: {}", labels.stream().map(Label::getName).toList());
                return labels;
            } else {
                log.warn("Received null response when fetching labels");
                return List.of();
            }

        } catch (FeignException e) {
            log.error("GitHub API error when fetching labels: {} - {}", e.status(), e.getMessage());
            return List.of();
        } catch (Exception e) {
            log.error("Error fetching repository labels: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Extracts owner and repository name from a GitHub URL
     *
     * @param githubUrl The GitHub URL (repository or issue)
     * @return Array with [owner, repo] or null if extraction fails
     */
    private String[] extractOwnerAndRepo(String githubUrl) {
        if (githubUrl == null || githubUrl.isEmpty()) {
            return null;
        }

        try {
            // Handle different URL types:
            // Repository URL format: https://api.github.com/repos/owner/repo
            // Issue URL format: https://api.github.com/repos/owner/repo/issues/123

            String path;
            if (githubUrl.startsWith("https://api.github.com")) {
                // Remove the base URL to get the path
                path = githubUrl.replace("https://api.github.com", "");
            } else {
                // Assume it's already a path
                path = githubUrl;
            }

            // Remove leading slash if present
            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            // Expected format: repos/owner/repo or repos/owner/repo/issues/123
            String[] parts = path.split("/");
            if (parts.length >= 3 && "repos".equals(parts[0])) {
                return new String[]{parts[1], parts[2]};
            }

            log.warn("Unexpected GitHub URL format: {}", githubUrl);
            return null;

        } catch (Exception e) {
            log.error("Error extracting owner and repo from URL '{}': {}", githubUrl, e.getMessage());
            return null;
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
            // Extract owner and repo from the URL
            String[] ownerAndRepo = extractOwnerAndRepo(repositoryUrl);
            if (ownerAndRepo == null) {
                log.warn("Could not extract owner and repo from URL: {}", repositoryUrl);
                return List.of();
            }

            String owner = ownerAndRepo[0];
            String repo = ownerAndRepo[1];
            log.info("Fetching issues from repository: {}/{}", owner, repo);

            List<GitHubIssue> allIssues = new ArrayList<>();
            int page = 1;
            int maxPages = 5; // Limit to first 5 pages (500 issues max) for LLM context
            int perPage = 100;

            while (page <= maxPages) {
                log.debug("Fetching issues from page {} for repository {}/{}", page, owner, repo);

                // Make the API call to fetch issues for this page using Feign client
                List<GitHubIssue> pageIssues = gitHubFeignClient.getRepositoryIssues(
                    owner, repo, "all", "updated", "desc", perPage, page
                );

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

        } catch (FeignException e) {
            log.error("GitHub API error when fetching issues: {} - {}", e.status(), e.getMessage());
            return List.of();
        } catch (Exception e) {
            log.error("Error fetching repository issues: {}", e.getMessage(), e);
            return List.of();
        }
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
