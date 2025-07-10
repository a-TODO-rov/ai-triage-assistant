package com.redis.triage.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Model representing a GitHub issue payload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitHubIssuePayload {
    
    /**
     * The title of the GitHub issue
     */
    private String title;
    
    /**
     * The body/description of the GitHub issue
     */
    private String body;
    
    /**
     * Additional properties from the GitHub webhook payload
     */
    private Map<String, Object> additional;
}
