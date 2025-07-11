package com.redis.triage.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing a similar issue found through semantic search
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimilarIssue {
    
    /**
     * The unique identifier of the issue
     */
    private String issueId;
    
    /**
     * The title of the similar issue
     */
    private String title;
    
    /**
     * The labels associated with the similar issue
     */
    private List<String> labels;
}
