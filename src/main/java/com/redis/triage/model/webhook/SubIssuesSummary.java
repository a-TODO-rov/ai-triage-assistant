package com.redis.triage.model.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model representing sub-issues summary from webhook payload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubIssuesSummary {
    
    /**
     * The total number of sub-issues
     */
    private Integer total;
    
    /**
     * The number of completed sub-issues
     */
    private Integer completed;
    
    /**
     * The percentage of completed sub-issues
     */
    @JsonProperty("percent_completed")
    private Integer percentCompleted;
}
