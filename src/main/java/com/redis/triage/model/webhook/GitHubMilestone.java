package com.redis.triage.model.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Model representing a GitHub milestone from webhook payload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitHubMilestone {
    
    /**
     * The API URL of the milestone
     */
    private String url;
    
    /**
     * The HTML URL of the milestone
     */
    @JsonProperty("html_url")
    private String htmlUrl;
    
    /**
     * The labels URL of the milestone
     */
    @JsonProperty("labels_url")
    private String labelsUrl;
    
    /**
     * The unique identifier of the milestone
     */
    private Long id;
    
    /**
     * The node ID of the milestone
     */
    @JsonProperty("node_id")
    private String nodeId;
    
    /**
     * The number of the milestone
     */
    private Integer number;
    
    /**
     * The title of the milestone
     */
    private String title;
    
    /**
     * The description of the milestone
     */
    private String description;
    
    /**
     * The creator of the milestone
     */
    private GitHubUser creator;
    
    /**
     * The number of open issues in the milestone
     */
    @JsonProperty("open_issues")
    private Integer openIssues;
    
    /**
     * The number of closed issues in the milestone
     */
    @JsonProperty("closed_issues")
    private Integer closedIssues;
    
    /**
     * The state of the milestone (open, closed)
     */
    private String state;
    
    /**
     * When the milestone was created
     */
    @JsonProperty("created_at")
    private Instant createdAt;
    
    /**
     * When the milestone was last updated
     */
    @JsonProperty("updated_at")
    private Instant updatedAt;
    
    /**
     * The due date of the milestone
     */
    @JsonProperty("due_on")
    private Instant dueOn;
    
    /**
     * When the milestone was closed
     */
    @JsonProperty("closed_at")
    private Instant closedAt;
}
