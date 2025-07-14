package com.redis.triage.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Model representing a GitHub issue from webhook payload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitHubIssue {
    
    /**
     * The API URL of the issue
     */
    private String url;
    
    /**
     * The repository URL
     */
    @JsonProperty("repository_url")
    private String repositoryUrl;
    
    /**
     * The labels URL template
     */
    @JsonProperty("labels_url")
    private String labelsUrl;
    
    /**
     * The comments URL
     */
    @JsonProperty("comments_url")
    private String commentsUrl;
    
    /**
     * The events URL
     */
    @JsonProperty("events_url")
    private String eventsUrl;
    
    /**
     * The HTML URL of the issue (web interface)
     */
    @JsonProperty("html_url")
    private String htmlUrl;
    
    /**
     * The unique identifier of the issue
     */
    private Long id;
    
    /**
     * The node ID of the issue
     */
    @JsonProperty("node_id")
    private String nodeId;
    
    /**
     * The issue number within the repository
     */
    private Integer number;
    
    /**
     * The title of the issue
     */
    private String title;
    
    /**
     * The user who created the issue
     */
    private GitHubUser user;
    
    /**
     * The labels assigned to the issue
     */
    private List<GitHubLabel> labels;
    
    /**
     * The state of the issue (open, closed)
     */
    private String state;
    
    /**
     * Whether the issue is locked
     */
    private Boolean locked;
    
    /**
     * The user assigned to the issue
     */
    private GitHubUser assignee;
    
    /**
     * List of users assigned to the issue
     */
    private List<GitHubUser> assignees;
    
    /**
     * The milestone associated with the issue
     */
    private GitHubMilestone milestone;
    
    /**
     * Number of comments on the issue
     */
    private Integer comments;
    
    /**
     * When the issue was created
     */
    @JsonProperty("created_at")
    private Instant createdAt;
    
    /**
     * When the issue was last updated
     */
    @JsonProperty("updated_at")
    private Instant updatedAt;
    
    /**
     * When the issue was closed (null if still open)
     */
    @JsonProperty("closed_at")
    private Instant closedAt;
    
    /**
     * The author's association with the repository
     */
    @JsonProperty("author_association")
    private String authorAssociation;
    
    /**
     * The reason for the active lock (if locked)
     */
    @JsonProperty("active_lock_reason")
    private String activeLockReason;
    
    /**
     * Summary of sub-issues
     */
    @JsonProperty("sub_issues_summary")
    private SubIssuesSummary subIssuesSummary;
    
    /**
     * The body/description of the issue
     */
    private String body;
    
    /**
     * Reactions to the issue
     */
    private GitHubReactions reactions;
    
    /**
     * The timeline URL
     */
    @JsonProperty("timeline_url")
    private String timelineUrl;
    
    /**
     * The GitHub app that performed the action (if applicable)
     */
    @JsonProperty("performed_via_github_app")
    private Object performedViaGithubApp;
    
    /**
     * The reason for the current state
     */
    @JsonProperty("state_reason")
    private String stateReason;
}
