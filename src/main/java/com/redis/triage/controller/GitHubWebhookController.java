package com.redis.triage.controller;

import com.redis.triage.model.GitHubIssuePayload;
import com.redis.triage.service.LabelingService;
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

        // Generate labels for the issue
        List<String> labels = labelingService.generateLabels(issue);
        log.info("Generated labels for issue '{}': {}", issue.getTitle(), labels);

        // Send notification to Slack
        slackNotifier.sendNotification(issue, labels);

        return Map.of("status", "labels applied");
    }
}
