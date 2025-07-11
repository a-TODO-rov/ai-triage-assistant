package com.redis.triage.controller;

import com.redis.triage.model.GitHubIssuePayload;
import com.redis.triage.model.SimilarIssue;
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

    /**
     * Handles GitHub issue webhook events
     *
     * @param issue The GitHub issue payload
     * @return Response indicating successful processing
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> handleIssueWebhook(@RequestBody GitHubIssuePayload issue) {
        log.info("Received GitHub issue webhook for: {}", issue.getTitle());

        try {
            // Generate labels for the issue
            List<String> labels = labelingService.generateLabels(issue);
            log.info("Generated labels for issue '{}': {}", issue.getTitle(), labels);

            // Atomically find similar issues and store the new issue
            List<SimilarIssue> similarIssues = semanticSearchService.findSimilarIssuesAndStore(issue, labels, 3);
            log.info("Found {} similar issues and stored new issue '{}': {}",
                similarIssues.size(), issue.getTitle(),
                similarIssues.stream().map(SimilarIssue::getTitle).toList());

            // Send notification to Slack with similar issues
            slackNotifier.sendNotification(issue, labels, similarIssues);
            log.info("Successfully sent Slack notification with similar issues for: {}", issue.getTitle());

            return Map.of(
                "status", "labels applied",
                "labels_count", String.valueOf(labels.size()),
                "similar_issues_count", String.valueOf(similarIssues.size()),
                "stored_in_redis", "true"
            );

        } catch (Exception e) {
            log.error("Error processing GitHub issue webhook for '{}': {}", issue.getTitle(), e.getMessage(), e);
            return Map.of(
                "status", "error",
                "message", "Failed to process issue: " + e.getMessage()
            );
        }
    }

}
