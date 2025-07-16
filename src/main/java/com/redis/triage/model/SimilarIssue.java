package com.redis.triage.model;

import com.redis.triage.model.webhook.GitHubIssue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model representing a similar issue with its similarity score
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimilarIssue {
    
    /**
     * The GitHub issue that was found to be similar
     */
    private GitHubIssue issue;
    
    /**
     * The similarity score (0.0 to 1.0, where 1.0 is identical)
     * This represents the cosine similarity between vector embeddings
     */
    private double similarityScore;
}
