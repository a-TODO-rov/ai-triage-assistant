package com.redis.triage.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simple model for storing issue title and body for LLM context
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueContext {
    
    /**
     * The title of the issue
     */
    private String title;
    
    /**
     * The body/description of the issue
     */
    private String body;
}
