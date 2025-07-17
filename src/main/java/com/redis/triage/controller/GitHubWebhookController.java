package com.redis.triage.controller;

import com.redis.triage.service.chain.TriageProcessorFacade;
import com.redis.triage.model.webhook.GitHubWebhookPayload;
import com.redis.triage.model.webhook.GitHubIssue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * Controller for handling GitHub webhook events
 */
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
@Slf4j
public class GitHubWebhookController {

    private final TriageProcessorFacade triageProcessorFacade;

    /**
     * Handles GitHub issue webhook events
     *
     * @param webhookPayload The complete GitHub webhook payload
     * @return Response indicating successful processing
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> handleIssueWebhook(@RequestBody GitHubWebhookPayload webhookPayload) {
        // Validate webhook payload
        if (Objects.isNull(webhookPayload) || Objects.isNull(webhookPayload.getIssue())) {
            log.error("Received invalid webhook payload: missing issue data");
            ResponseEntity.badRequest().build();
        }

        // Extract the issue from the webhook payload
        GitHubIssue issue = webhookPayload.getIssue();

        log.info("Received GitHub issue webhook for: {} (action: {})",
                issue.getTitle(), webhookPayload.getAction());

        try {
            triageProcessorFacade.process(issue);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing GitHub issue webhook for '{}': {}",
                issue.getTitle(), e.getMessage(), e);
            return  ResponseEntity.internalServerError().build();
        }
    }
}
