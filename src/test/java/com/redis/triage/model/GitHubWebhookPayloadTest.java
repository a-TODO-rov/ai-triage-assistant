package com.redis.triage.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.redis.triage.model.webhook.*;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for GitHubWebhookPayload model parsing
 */
class GitHubWebhookPayloadTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    void shouldParseWebhookPayloadFromJson() throws IOException {
        // Given: Load the webhook payload JSON
        String webhookJson = loadWebhookPayload();
        
        // When: Parse the JSON into the model
        GitHubWebhookPayload payload = objectMapper.readValue(webhookJson, GitHubWebhookPayload.class);
        
        // Then: Verify the payload structure
        assertThat(payload).isNotNull();
        assertThat(payload.getAction()).isEqualTo("opened");
        
        // Verify issue data
        GitHubIssue issue = payload.getIssue();
        assertThat(issue).isNotNull();
        assertThat(issue.getId()).isEqualTo(3228322419L);
        assertThat(issue.getNumber()).isEqualTo(2);
        assertThat(issue.getTitle()).isEqualTo("---TEST2---");
        assertThat(issue.getState()).isEqualTo("open");
        assertThat(issue.getHtmlUrl()).isEqualTo("https://github.com/a-TODO-rov/ai-triage-assistant/issues/2");
        assertThat(issue.getBody()).contains("Bug Report");
        assertThat(issue.getBody()).contains("migrating a slot");
        assertThat(issue.getBody()).contains("Lettuce");
        assertThat(issue.getCreatedAt()).isEqualTo(Instant.parse("2025-07-14T10:53:57Z"));
        assertThat(issue.getUpdatedAt()).isEqualTo(Instant.parse("2025-07-14T10:53:57Z"));
        assertThat(issue.getClosedAt()).isNull();
        assertThat(issue.getAuthorAssociation()).isEqualTo("OWNER");
        
        // Verify user data
        GitHubUser user = issue.getUser();
        assertThat(user).isNotNull();
        assertThat(user.getLogin()).isEqualTo("a-TODO-rov");
        assertThat(user.getId()).isEqualTo(218597349L);
        assertThat(user.getType()).isEqualTo("User");
        assertThat(user.getSiteAdmin()).isFalse();
        
        // Verify repository data
        GitHubRepository repository = payload.getRepository();
        assertThat(repository).isNotNull();
        assertThat(repository.getId()).isEqualTo(1018048672L);
        assertThat(repository.getName()).isEqualTo("ai-triage-assistant");
        assertThat(repository.getFullName()).isEqualTo("a-TODO-rov/ai-triage-assistant");
        assertThat(repository.getIsPrivate()).isFalse();
        assertThat(repository.getLanguage()).isEqualTo("Java");
        assertThat(repository.getDefaultBranch()).isEqualTo("main");
        
        // Verify repository owner
        GitHubUser owner = repository.getOwner();
        assertThat(owner).isNotNull();
        assertThat(owner.getLogin()).isEqualTo("a-TODO-rov");
        assertThat(owner.getId()).isEqualTo(218597349L);
        
        // Verify sender
        GitHubUser sender = payload.getSender();
        assertThat(sender).isNotNull();
        assertThat(sender.getLogin()).isEqualTo("a-TODO-rov");
        assertThat(sender.getId()).isEqualTo(218597349L);
        
        // Verify sub-issues summary
        SubIssuesSummary subIssues = issue.getSubIssuesSummary();
        assertThat(subIssues).isNotNull();
        assertThat(subIssues.getTotal()).isEqualTo(0);
        assertThat(subIssues.getCompleted()).isEqualTo(0);
        assertThat(subIssues.getPercentCompleted()).isEqualTo(0);
        
        // Verify reactions
        GitHubReactions reactions = issue.getReactions();
        assertThat(reactions).isNotNull();
        assertThat(reactions.getTotalCount()).isEqualTo(0);
        assertThat(reactions.getPlusOne()).isEqualTo(0);
        assertThat(reactions.getMinusOne()).isEqualTo(0);
        
        // Verify labels (should be empty array)
        assertThat(issue.getLabels()).isNotNull();
        assertThat(issue.getLabels()).isEmpty();
        
        // Verify assignees (should be empty)
        assertThat(issue.getAssignees()).isNotNull();
        assertThat(issue.getAssignees()).isEmpty();
        assertThat(issue.getAssignee()).isNull();
        assertThat(issue.getMilestone()).isNull();
    }
    
    @Test
    void shouldExtractIssueFromWebhookPayload() throws IOException {
        // Given: Load and parse the webhook payload
        String webhookJson = loadWebhookPayload();
        GitHubWebhookPayload payload = objectMapper.readValue(webhookJson, GitHubWebhookPayload.class);
        
        // When: Extract the issue
        GitHubIssue issue = payload.getIssue();
        
        // Then: Verify we can work with the issue directly
        assertThat(issue).isNotNull();
        assertThat(issue.getTitle()).isEqualTo("---TEST2---");
        assertThat(issue.getId()).isEqualTo(3228322419L);
        assertThat(issue.getNumber()).isEqualTo(2);
        assertThat(issue.getHtmlUrl()).isEqualTo("https://github.com/a-TODO-rov/ai-triage-assistant/issues/2");
        
        // Verify this is the same data that would be used by services
        String expectedSearchText = String.format("Title: %s\nBody: %s", 
            issue.getTitle(), issue.getBody());
        assertThat(expectedSearchText).contains("---TEST2---");
        assertThat(expectedSearchText).contains("Bug Report");
        
        // Verify issue ID extraction
        String issueId = String.valueOf(issue.getId());
        assertThat(issueId).isEqualTo("3228322419");
    }

    private String loadWebhookPayload() throws IOException {
        ClassPathResource resource = new ClassPathResource("request/webhook_payload.json");
        return Files.readString(resource.getFile().toPath());
    }
}
