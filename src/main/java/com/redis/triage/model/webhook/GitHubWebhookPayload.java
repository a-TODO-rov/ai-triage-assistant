package com.redis.triage.model.webhook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model representing the complete GitHub webhook payload for issue events
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitHubWebhookPayload {
    
    /**
     * The action that was performed on the issue (e.g., "opened", "closed", "edited")
     */
    private String action;
    
    /**
     * The issue that triggered the webhook
     */
    private GitHubIssue issue;
    
    /**
     * The repository where the issue was created
     */
    private GitHubRepository repository;
    
    /**
     * The user who triggered the webhook event
     */
    private GitHubUser sender;
}
