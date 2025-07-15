package com.redis.triage.service;

import com.redis.triage.model.GitHubIssue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for generating AI-powered summaries of GitHub issues
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AISummaryService {

    private final LiteLLMClient liteLLMClient;

    /**
     * Generates an AI summary for a GitHub issue
     *
     * @param issue The GitHub issue to summarize
     * @param labels The generated labels for the issue (optional context)
     * @return AI-generated summary of the issue
     */
    public String generateSummary(GitHubIssue issue, List<String> labels) {
        log.info("Generating AI summary for issue: {}", issue.getTitle());

        try {
            // Build the prompt for summary generation
            String prompt = buildSummaryPrompt(issue, labels);
            log.debug("Built summary prompt for issue '{}': {}", issue.getTitle(), prompt);

            // Call LiteLLM to get the summary
            String response = liteLLMClient.callLLM(prompt);
            log.debug("Received summary response from LiteLLM: {}", response);

            // Clean up the response
            String summary = cleanSummaryResponse(response);
            log.info("Generated summary for issue '{}': {}", issue.getTitle(), summary);

            return summary;

        } catch (Exception e) {
            log.error("Error generating summary for issue '{}': {}", issue.getTitle(), e.getMessage(), e);
            return "Unable to generate summary at this time.";
        }
    }

    /**
     * Builds the prompt for AI summary generation
     *
     * @param issue The GitHub issue
     * @param labels The generated labels for context
     * @return The formatted prompt string
     */
    private String buildSummaryPrompt(GitHubIssue issue, List<String> labels) {
        String labelsContext = labels != null && !labels.isEmpty() ? 
            String.join(", ", labels) : "none";

        return String.format("""
            You are an AI assistant helping with GitHub issue triage. Please provide a concise, professional summary of the following GitHub issue.
            
            The summary should:
            - Be 1-3 sentences maximum
            - Focus on the core problem or request
            - Be written in a clear, technical tone
            - Avoid unnecessary details
            - Help maintainers quickly understand the issue
            
            Issue Details:
            ---
            Title: %s
            Body: %s
            Generated Labels: %s
            ---
            
            Provide only the summary, no additional text or formatting.""",
            issue.getTitle() != null ? issue.getTitle() : "No title",
            issue.getBody() != null ? issue.getBody() : "No description provided",
            labelsContext
        );
    }

    /**
     * Cleans up the AI response to ensure it's suitable for Slack
     *
     * @param response The raw response from the AI
     * @return Cleaned summary text
     */
    private String cleanSummaryResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "No summary available.";
        }

        // Remove common AI response prefixes/suffixes
        String cleaned = response.trim()
            .replaceAll("^(Summary:|Here's a summary:|The summary is:)\\s*", "")
            .replaceAll("\\s*(\\.|Summary complete\\.?)$", "")
            .trim();

        // Ensure it doesn't start with quotes
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }

        // Limit length to prevent overly long summaries
        if (cleaned.length() > 500) {
            cleaned = cleaned.substring(0, 497) + "...";
        }

        return cleaned.isEmpty() ? "No summary available." : cleaned;
    }

    /**
     * Generates a summary with similar issues context
     *
     * @param issue The GitHub issue to summarize
     * @param labels The generated labels for the issue
     * @param similarIssues List of similar issues for additional context
     * @return AI-generated summary with context from similar issues
     */
    public String generateSummaryWithContext(GitHubIssue issue, List<String> labels, List<GitHubIssue> similarIssues) {
        log.info("Generating AI summary with similar issues context for: {}", issue.getTitle());

        try {
            // Build the prompt with similar issues context
            String prompt = buildSummaryPromptWithContext(issue, labels, similarIssues);
            log.debug("Built contextual summary prompt for issue '{}': {}", issue.getTitle(), prompt);

            // Call LiteLLM to get the summary
            String response = liteLLMClient.callLLM(prompt);
            log.debug("Received contextual summary response from LiteLLM: {}", response);

            // Clean up the response
            String summary = cleanSummaryResponse(response);
            log.info("Generated contextual summary for issue '{}': {}", issue.getTitle(), summary);

            return summary;

        } catch (Exception e) {
            log.error("Error generating contextual summary for issue '{}': {}", issue.getTitle(), e.getMessage(), e);
            // Fallback to basic summary
            return generateSummary(issue, labels);
        }
    }

    /**
     * Builds the prompt for AI summary generation with similar issues context
     *
     * @param issue The GitHub issue
     * @param labels The generated labels for context
     * @param similarIssues List of similar issues for additional context
     * @return The formatted prompt string with context
     */
    private String buildSummaryPromptWithContext(GitHubIssue issue, List<String> labels, List<GitHubIssue> similarIssues) {
        String labelsContext = labels != null && !labels.isEmpty() ? 
            String.join(", ", labels) : "none";

        StringBuilder similarIssuesContext = new StringBuilder();
        if (similarIssues != null && !similarIssues.isEmpty()) {
            similarIssuesContext.append("\n\nSimilar Issues Found:\n");
            for (int i = 0; i < Math.min(similarIssues.size(), 3); i++) {
                GitHubIssue similar = similarIssues.get(i);
                similarIssuesContext.append(String.format("- %s\n", 
                    similar.getTitle() != null ? similar.getTitle() : "Unknown Issue"));
            }
        }

        return String.format("""
            You are an AI assistant helping with GitHub issue triage. Please provide a concise, professional summary of the following GitHub issue.
            
            The summary should:
            - Be 1-3 sentences maximum
            - Focus on the core problem or request
            - Be written in a clear, technical tone
            - Consider the context of similar issues if relevant
            - Help maintainers quickly understand the issue
            
            Issue Details:
            ---
            Title: %s
            Body: %s
            Generated Labels: %s%s
            ---
            
            Provide only the summary, no additional text or formatting.""",
            issue.getTitle() != null ? issue.getTitle() : "No title",
            issue.getBody() != null ? issue.getBody() : "No description provided",
            labelsContext,
            similarIssuesContext.toString()
        );
    }
}
