package com.redis.triage.service;

import com.redis.triage.model.GitHubIssue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Service for sending notifications to Slack via webhook
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SlackNotifier {

    @Qualifier("slackWebClient")
    private final WebClient slackWebClient;

    /**
     * Sends a notification about a GitHub issue with generated labels and similar issues
     *
     * @param issue The GitHub issue
     * @param labels The generated labels for the issue
     * @param similarIssues List of similar issues found through semantic search
     */
    public void sendNotification(GitHubIssue issue, List<String> labels, List<GitHubIssue> similarIssues) {
        log.info("Sending Slack notification for issue: {}", issue.getTitle());

        try {
            // Compose the Slack message with similar issues
            String message = composeSlackMessageWithSimilarIssues(issue, labels, similarIssues);
            log.debug("Composed Slack message: {}", message);

            // Build JSON payload
            Map<String, String> payload = Map.of("text", message);

            // Send the notification
            Mono<String> responseMono = slackWebClient
                .post()
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class);

            String response = responseMono.block();
            log.info("Successfully sent Slack notification for issue: {}", issue.getTitle());
            log.debug("Slack API response: {}", response);

        } catch (Exception e) {
            log.error("Failed to send Slack notification for issue '{}': {}",
                issue.getTitle(), e.getMessage(), e);
        }
    }

    /**
     * Composes a formatted Slack message for a GitHub issue with similar issues
     *
     * @param issue The GitHub issue
     * @param labels The generated labels for the issue
     * @param similarIssues List of similar issues found through semantic search
     * @return Formatted Slack message
     */
    private String composeSlackMessageWithSimilarIssues(GitHubIssue issue, List<String> labels, List<GitHubIssue> similarIssues) {
        String title = issue.getTitle() != null ? issue.getTitle() : "Untitled Issue";
        String labelsText = labels != null && !labels.isEmpty() ?
            String.join(", ", labels) : "No labels";
        String link = issue.getHtmlUrl() != null ?
            issue.getHtmlUrl() : "https://github.com/placeholder/issue";

        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(String.format(
            "🚨 *New GitHub Issue Labeled*\n\n" +
            "📋 *Title:* %s\n" +
            "🏷️ *Labels:* %s\n" +
            "🔗 *Link:* %s",
            title, labelsText, link
        ));

        // Add similar issues if any found
        if (similarIssues != null && !similarIssues.isEmpty()) {
            messageBuilder.append("\n\n🔍 *Similar Issues Found:*");
            for (int i = 0; i < Math.min(similarIssues.size(), 3); i++) { // Limit to top 3
                GitHubIssue similar = similarIssues.get(i);
                String similarLabels = similar.getLabels() != null && !similar.getLabels().isEmpty() ?
                    similar.getLabels().stream()
                            .map(label -> label.getName())
                            .collect(java.util.stream.Collectors.joining(", ")) : "No labels";
                messageBuilder.append(String.format(
                    "\n• *#%s:* %s (Labels: %s)",
                    similar.getId(),
                    similar.getTitle(),
                    similarLabels
                ));
            }
        }

        return messageBuilder.toString();
    }
}
