package com.redis.triage.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for handling GitHub webhook events
 */
@RestController
public class GitHubWebhookController {

    /**
     * Handles GitHub webhook POST requests
     * 
     * @param payload The webhook payload from GitHub
     * @return Response indicating successful processing
     */
    @PostMapping("/webhook")
    public String handleWebhook(@RequestBody String payload) {
        // TODO: Implement webhook handling logic
        return "Webhook received";
    }
}
