package com.redis.triage.service;

import com.redis.triage.client.SlackFeignClient;
import com.redis.triage.model.GitHubSimilarIssue;
import com.redis.triage.model.feign.SlackWebhookRequest;
import com.redis.triage.model.webhook.GitHubIssue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for sending notifications to Slack via webhook
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SlackNotifier {

    private final SlackFeignClient slackFeignClient;

    /**
     * Sends a notification about a GitHub issue with generated labels and similar issues
     *
     * @param issue The GitHub issue
     * @param labels The generated labels for the issue
     * @param similarIssues List of similar issues found through semantic search
     */
    public void sendNotification(GitHubIssue issue, List<String> labels, List<GitHubSimilarIssue> similarIssues) {
        sendNotification(issue, labels, similarIssues, null);
    }

    /**
     * Sends a notification about a GitHub issue with generated labels, similar issues, and AI summary
     *
     * @param issue The GitHub issue
     * @param labels The generated labels for the issue
     * @param similarIssues List of similar issues found through semantic search
     * @param aiSummary AI-generated summary of the issue (optional)
     */
    public void sendNotification(GitHubIssue issue, List<String> labels, List<GitHubSimilarIssue> similarIssues, String aiSummary) {
        log.info("Sending Slack notification for issue: {}", issue.getTitle());

        try {
            // Compose the Slack message with similar issues and AI summary
            String message = composeSlackMessage(issue, labels, similarIssues, aiSummary);
            log.debug("Composed Slack message: {}", message);

            // Build request
            SlackWebhookRequest request = SlackWebhookRequest.builder()
                .text(message)
                .build();

            // Send the notification
            String response = slackFeignClient.sendMessage(request);
            log.info("Successfully sent Slack notification for issue: {}", issue.getTitle());
            log.debug("Slack API response: {}", response);

        } catch (Exception e) {
            log.error("Failed to send Slack notification for issue '{}': {}",
                issue.getTitle(), e.getMessage(), e);
        }
    }

    /**
     * Composes a formatted Slack message for a GitHub issue with similar issues and AI summary
     *
     * @param issue The GitHub issue
     * @param labels The generated labels for the issue
     * @param similarIssues List of similar issues found through semantic search
     * @param aiSummary AI-generated summary of the issue (optional)
     * @return Formatted Slack message
     */
    private String composeSlackMessage(GitHubIssue issue, List<String> labels, List<GitHubSimilarIssue> similarIssues, String aiSummary) {
        StringBuilder messageBuilder = new StringBuilder();

        // Header
        messageBuilder.append(":triangular_flag_on_post: *New GitHub Issue Received*\n\n");

        // Title with link
        String title = issue.getTitle() != null ? issue.getTitle() : "Untitled Issue";
        String issueUrl = issue.getHtmlUrl() != null ? issue.getHtmlUrl() : "";
        messageBuilder.append(String.format("*🔗 Title:* [%s](%s)\n\n", title, issueUrl));

        // Labels section
        if (labels != null && !labels.isEmpty()) {
            messageBuilder.append("*🏷️ Labels:* ");
            for (int i = 0; i < labels.size(); i++) {
                messageBuilder.append(String.format("`%s`", labels.get(i)));
                if (i < labels.size() - 1) {
                    messageBuilder.append(" · ");
                }
            }
            messageBuilder.append("\n\n");
        }

        // AI Summary section
        if (aiSummary != null && !aiSummary.trim().isEmpty()) {
            messageBuilder.append("*🧠 AI Summary:*  \n");
            // Handle multi-line summaries by prefixing each line with "> "
            String[] summaryLines = aiSummary.trim().split("\n");
            for (String line : summaryLines) {
                messageBuilder.append("> ").append(line).append("\n");
            }
            messageBuilder.append("\n");
        }

        // Similar Issues section
        if (similarIssues != null && !similarIssues.isEmpty()) {
            messageBuilder.append("*🧩 Similar Issues:*  \n");
            for (int i = 0; i < Math.min(similarIssues.size(), 3); i++) { // Limit to top 3
                GitHubSimilarIssue similar = similarIssues.get(i);

                int score = similar.similarityScore();

                String issueNumber = similar.issue().getNumber() != null ?
                    similar.issue().getNumber().toString() : similar.issue().getId().toString();
                String similarUrl = similar.issue().getUrl() != null ?
                    similar.issue().getUrl() : "";
                String similarTitle = similar.issue().getTitle() != null ?
                    similar.issue().getTitle() : "Unknown Issue";

                messageBuilder.append(String.format("• [#%s](%s) – \"%s\" (%d%%)",
                    issueNumber, similarUrl, similarTitle, score));

                if (i < Math.min(similarIssues.size(), 3) - 1) {
                    messageBuilder.append("  \n");
                }
            }
            messageBuilder.append("\n\n");
        }

        return messageBuilder.toString();
    }
}
