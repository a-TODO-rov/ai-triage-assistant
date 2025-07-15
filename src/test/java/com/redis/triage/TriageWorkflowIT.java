package com.redis.triage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.triage.controller.GitHubWebhookController;
import com.redis.triage.model.webhook.GitHubIssue;
import com.redis.triage.model.webhook.GitHubWebhookPayload;
import com.redis.triage.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration test for the complete issue triage workflow
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class TriageWorkflowIT {

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>("redis/redis-stack-server:latest")
            .withExposedPorts(6379);

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisVectorStoreService redisVectorStoreService;

    @Autowired
    private SemanticSearchService semanticSearchService;

    @Autowired
    private LabelingService labelingService;

    @Autowired
    private GitHubWebhookController gitHubWebhookController;

    @MockBean
    private LiteLLMClient liteLLMClient;

    @MockBean
    private SlackNotifier slackNotifier;

    // Test data
    private static final float[] MOCK_EMBEDDING = createMockEmbedding();
    private static final List<String> EXPECTED_LABELS = List.of("bug", "lettuce");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
    }

    @BeforeEach
    void setUp() {
        // Reset mocks
        reset(liteLLMClient, slackNotifier);
        
        // Setup mock responses
        setupMockResponses();
        
        // Insert mock issues into Redis
        insertMockIssuesIntoRedis();
    }

    @Test
    void shouldExecuteCompleteTriageWorkflow() throws Exception {
        // Given: Load the webhook payload from file
        String webhookJson = loadWebhookPayload();

        // Parse to get the issue data for assertions
        GitHubWebhookPayload webhookPayload = objectMapper.readValue(webhookJson, GitHubWebhookPayload.class);
        GitHubIssue issue = webhookPayload.getIssue();

        // When: POST to webhook endpoint
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(webhookJson, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/webhook",
                request,
                Map.class
        );

        // Then: Verify the complete workflow
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsEntry("status", "labels applied");
        assertThat(response.getBody()).containsKey("labels_count");
        assertThat(response.getBody()).containsKey("similar_issues_count");
        assertThat(response.getBody()).containsEntry("stored_in_redis", "true");
        assertThat(response.getBody()).containsEntry("action", "opened");
        assertThat(response.getBody()).containsEntry("repository", "a-TODO-rov/ai-triage-assistant");
        assertThat(response.getBody()).containsEntry("issue_id", String.valueOf(issue.getId()));
        assertThat(response.getBody()).containsEntry("issue_number", String.valueOf(issue.getNumber()));

        // Verify LiteLLMClient was called for embedding generation
        verify(liteLLMClient, times(1)).generateEmbedding(
                argThat(text -> text.contains(issue.getTitle()) && text.contains(issue.getBody()))
        );

        // Verify LiteLLMClient was called for label generation
        verify(liteLLMClient, times(1)).callLLM(
                argThat(prompt -> prompt.contains("AI triage assistant") &&
                                prompt.contains(issue.getTitle()))
        );

        // Verify SlackNotifier was called with correct message including similar issues
        ArgumentCaptor<GitHubIssue> issueCaptor = ArgumentCaptor.forClass(GitHubIssue.class);
        ArgumentCaptor<List<String>> labelsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<GitHubIssue>> similarIssuesCaptor = ArgumentCaptor.forClass(List.class);

        verify(slackNotifier, times(1)).sendNotification(
                issueCaptor.capture(),
                labelsCaptor.capture(),
                similarIssuesCaptor.capture()
        );

        // Verify captured arguments
        GitHubIssue capturedIssue = issueCaptor.getValue();
        List<String> capturedLabels = labelsCaptor.getValue();
        List<GitHubIssue> capturedSimilarIssues = similarIssuesCaptor.getValue();

        assertThat(capturedIssue.getTitle()).isEqualTo(issue.getTitle());
        assertThat(capturedIssue.getId()).isEqualTo(issue.getId());
        assertThat(capturedLabels).containsExactlyInAnyOrderElementsOf(EXPECTED_LABELS);
        assertThat(capturedSimilarIssues).hasSize(2);
        assertThat(capturedSimilarIssues.get(0).getTitle()).isEqualTo("Redis cluster connection issues");
        assertThat(capturedSimilarIssues.get(1).getTitle()).isEqualTo("Jedis timeout in high load");

        // Verify semantic search functionality (using the read-only method to avoid double storage)
        List<GitHubIssue> similarIssues = semanticSearchService.findSimilarIssues(issue, 3);
        assertThat(similarIssues).hasSizeGreaterThanOrEqualTo(2);

        // Verify that we can find the original mock issues
        List<String> foundTitles = similarIssues.stream().map(GitHubIssue::getTitle).toList();
        assertThat(foundTitles).contains("Redis cluster connection issues");
        assertThat(foundTitles).contains("Jedis timeout in high load");

        // Verify labeling service functionality
        List<String> generatedLabels = labelingService.generateLabels(issue);
        assertThat(generatedLabels).containsExactlyInAnyOrderElementsOf(EXPECTED_LABELS);

        // Verify the new issue was stored in Redis and can be found in future searches
        // Wait a bit for Redis to process the new data
        Thread.sleep(200);

        // Create a slightly different issue to search for the stored one
        GitHubIssue searchIssue = GitHubIssue.builder()
                .title("Redis timeout issues in production environment")
                .body("Experiencing timeout problems with Redis cluster connections")
                .id(9999L)
                .number(9999)
                .htmlUrl("https://github.com/test/repo/issues/9999")
                .build();

        List<GitHubIssue> newSearchResults = semanticSearchService.findSimilarIssues(searchIssue, 5);
        // Should now find at least 3 issues: the 2 mock issues + the newly stored one
        assertThat(newSearchResults).hasSizeGreaterThanOrEqualTo(3);

        // Verify that the newly stored issue is now findable
        List<String> newFoundTitles = newSearchResults.stream().map(GitHubIssue::getTitle).toList();
        assertThat(newFoundTitles).contains(issue.getTitle());
    }

    private String loadWebhookPayload() throws IOException {
        ClassPathResource resource = new ClassPathResource("request/webhook_payload.json");
        return Files.readString(resource.getFile().toPath());
    }

    private void setupMockResponses() {
        // Mock embedding generation
        when(liteLLMClient.generateEmbedding(anyString())).thenReturn(MOCK_EMBEDDING);

        // Mock label generation
        when(liteLLMClient.callLLM(argThat(prompt -> prompt.contains("AI triage assistant"))))
                .thenReturn("bug, lettuce");

        // Mock Slack notification (void method)
        doNothing().when(slackNotifier).sendNotification(any(GitHubIssue.class), anyList(), anyList());
    }

    private void insertMockIssuesIntoRedis() {
        // Insert first mock issue
        redisVectorStoreService.storeEmbedding(
                "101",
                createSimilarEmbedding(0.1f),
                "Redis cluster connection issues",
                "Having trouble connecting to Redis cluster in production environment",
                List.of("bug", "redis-cluster"),
                "https://api.github.com/repos/a-TODO-rov/ai-triage-assistant/issues/1"
        );

        // Insert second mock issue
        redisVectorStoreService.storeEmbedding(
                "102",
                createSimilarEmbedding(0.2f),
                "Jedis timeout in high load",
                "Jedis client timing out under high load conditions",
                List.of("performance", "jedis"),
                "https://api.github.com/repos/a-TODO-rov/ai-triage-assistant/issues/2"
        );

        // Wait a bit for Redis to process the data
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static float[] createMockEmbedding() {
        float[] embedding = new float[1536]; // Standard OpenAI embedding dimension
        for (int i = 0; i < embedding.length; i++) {
            embedding[i] = (float) (Math.random() * 0.1); // Small random values
        }
        return embedding;
    }

    private static float[] createSimilarEmbedding(float offset) {
        float[] embedding = new float[1536];
        for (int i = 0; i < embedding.length; i++) {
            embedding[i] = MOCK_EMBEDDING[i] + offset; // Similar but slightly different
        }
        return embedding;
    }
}
