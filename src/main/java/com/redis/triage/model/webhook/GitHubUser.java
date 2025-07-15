package com.redis.triage.model.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model representing a GitHub user from webhook payload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitHubUser {
    
    /**
     * The username/login of the user
     */
    private String login;
    
    /**
     * The unique identifier of the user
     */
    private Long id;
    
    /**
     * The node ID of the user
     */
    @JsonProperty("node_id")
    private String nodeId;
    
    /**
     * The avatar URL of the user
     */
    @JsonProperty("avatar_url")
    private String avatarUrl;
    
    /**
     * The gravatar ID of the user
     */
    @JsonProperty("gravatar_id")
    private String gravatarId;
    
    /**
     * The API URL of the user
     */
    private String url;
    
    /**
     * The HTML URL of the user (profile page)
     */
    @JsonProperty("html_url")
    private String htmlUrl;
    
    /**
     * The followers URL
     */
    @JsonProperty("followers_url")
    private String followersUrl;
    
    /**
     * The following URL template
     */
    @JsonProperty("following_url")
    private String followingUrl;
    
    /**
     * The gists URL template
     */
    @JsonProperty("gists_url")
    private String gistsUrl;
    
    /**
     * The starred URL template
     */
    @JsonProperty("starred_url")
    private String starredUrl;
    
    /**
     * The subscriptions URL
     */
    @JsonProperty("subscriptions_url")
    private String subscriptionsUrl;
    
    /**
     * The organizations URL
     */
    @JsonProperty("organizations_url")
    private String organizationsUrl;
    
    /**
     * The repositories URL
     */
    @JsonProperty("repos_url")
    private String reposUrl;
    
    /**
     * The events URL template
     */
    @JsonProperty("events_url")
    private String eventsUrl;
    
    /**
     * The received events URL
     */
    @JsonProperty("received_events_url")
    private String receivedEventsUrl;
    
    /**
     * The type of user (User, Organization, etc.)
     */
    private String type;
    
    /**
     * The user view type
     */
    @JsonProperty("user_view_type")
    private String userViewType;
    
    /**
     * Whether the user is a site admin
     */
    @JsonProperty("site_admin")
    private Boolean siteAdmin;
}
