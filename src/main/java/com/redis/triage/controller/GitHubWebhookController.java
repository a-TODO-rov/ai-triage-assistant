package com.redis.triage.controller;

import com.redis.triage.model.GitHubWebhookPayload;
import com.redis.triage.model.GitHubIssue;
import com.redis.triage.service.GitHubService;
import com.redis.triage.service.LabelingService;
import com.redis.triage.service.SemanticSearchService;
import com.redis.triage.service.SlackNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Controller for handling GitHub webhook events
 */
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
@Slf4j
public class GitHubWebhookController {

    private final LabelingService labelingService;
    private final SemanticSearchService semanticSearchService;
    private final SlackNotifier slackNotifier;
    private final GitHubService gitHubService;

    /**
     * Handles GitHub issue webhook events
     *
     * @param webhookPayload The complete GitHub webhook payload
     * @return Response indicating successful processing
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> handleIssueWebhook(@RequestBody GitHubWebhookPayload webhookPayload) {
        // Validate webhook payload
        if (webhookPayload == null || webhookPayload.getIssue() == null) {
            log.error("Received invalid webhook payload: missing issue data");
            return Map.of(
                "status", "error",
                "message", "Invalid webhook payload: missing issue data"
            );
        }

        // Extract the issue from the webhook payload
        GitHubIssue issue = webhookPayload.getIssue();

        log.info("Received GitHub issue webhook for: {} (action: {})",
                issue.getTitle(), webhookPayload.getAction());

        try {
            // Generate labels for the issue with repository context
            String repositoryUrl = issue.getRepositoryUrl();
            List<String> labels = labelingService.generateLabels(issue, repositoryUrl);
            log.info("Generated labels for issue '{}': {}", issue.getTitle(), labels);

            // Atomically find similar issues and store the new issue
            List<GitHubIssue> similarIssues = semanticSearchService.findSimilarIssuesAndStore(issue, labels, 3);
            log.info("Found {} similar issues and stored new issue '{}': {}",
                similarIssues.size(), issue.getTitle(),
                similarIssues.stream().map(GitHubIssue::getTitle).toList());

            // Send notification to Slack with similar issues
            slackNotifier.sendNotification(issue, labels, similarIssues);
            log.info("Successfully sent Slack notification with similar issues for: {}", issue.getTitle());

            return Map.of(
                "status", "labels applied",
                "labels_count", String.valueOf(labels.size()),
                "similar_issues_count", String.valueOf(similarIssues.size()),
                "stored_in_redis", "true",
                "action", webhookPayload.getAction(),
                "repository", webhookPayload.getRepository() != null ?
                    webhookPayload.getRepository().getFullName() : "unknown",
                "issue_id", String.valueOf(issue.getId()),
                "issue_number", String.valueOf(issue.getNumber())
            );

        } catch (Exception e) {
            log.error("Error processing GitHub issue webhook for '{}': {}",
                issue.getTitle(), e.getMessage(), e);
            return Map.of(
                "status", "error",
                "message", "Failed to process issue: " + e.getMessage(),
                "action", webhookPayload.getAction()
            );
        }
    }

}
