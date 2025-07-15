package com.redis.triage.model.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Model representing a GitHub repository from webhook payload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitHubRepository {
    
    /**
     * The unique identifier of the repository
     */
    private Long id;
    
    /**
     * The node ID of the repository
     */
    @JsonProperty("node_id")
    private String nodeId;
    
    /**
     * The name of the repository
     */
    private String name;
    
    /**
     * The full name of the repository (owner/repo)
     */
    @JsonProperty("full_name")
    private String fullName;
    
    /**
     * Whether the repository is private
     */
    @JsonProperty("private")
    private Boolean isPrivate;
    
    /**
     * The owner of the repository
     */
    private GitHubUser owner;
    
    /**
     * The HTML URL of the repository
     */
    @JsonProperty("html_url")
    private String htmlUrl;
    
    /**
     * The description of the repository
     */
    private String description;
    
    /**
     * Whether the repository is a fork
     */
    private Boolean fork;
    
    /**
     * The API URL of the repository
     */
    private String url;
    
    /**
     * The forks URL
     */
    @JsonProperty("forks_url")
    private String forksUrl;
    
    /**
     * The keys URL template
     */
    @JsonProperty("keys_url")
    private String keysUrl;
    
    /**
     * The collaborators URL template
     */
    @JsonProperty("collaborators_url")
    private String collaboratorsUrl;
    
    /**
     * The teams URL
     */
    @JsonProperty("teams_url")
    private String teamsUrl;
    
    /**
     * The hooks URL
     */
    @JsonProperty("hooks_url")
    private String hooksUrl;
    
    /**
     * The issue events URL template
     */
    @JsonProperty("issue_events_url")
    private String issueEventsUrl;
    
    /**
     * The events URL
     */
    @JsonProperty("events_url")
    private String eventsUrl;
    
    /**
     * The assignees URL template
     */
    @JsonProperty("assignees_url")
    private String assigneesUrl;
    
    /**
     * The branches URL template
     */
    @JsonProperty("branches_url")
    private String branchesUrl;
    
    /**
     * The tags URL
     */
    @JsonProperty("tags_url")
    private String tagsUrl;
    
    /**
     * The blobs URL template
     */
    @JsonProperty("blobs_url")
    private String blobsUrl;
    
    /**
     * The git tags URL template
     */
    @JsonProperty("git_tags_url")
    private String gitTagsUrl;
    
    /**
     * The git refs URL template
     */
    @JsonProperty("git_refs_url")
    private String gitRefsUrl;
    
    /**
     * The trees URL template
     */
    @JsonProperty("trees_url")
    private String treesUrl;
    
    /**
     * The statuses URL template
     */
    @JsonProperty("statuses_url")
    private String statusesUrl;
    
    /**
     * The languages URL
     */
    @JsonProperty("languages_url")
    private String languagesUrl;
    
    /**
     * The stargazers URL
     */
    @JsonProperty("stargazers_url")
    private String stargazersUrl;
    
    /**
     * The contributors URL
     */
    @JsonProperty("contributors_url")
    private String contributorsUrl;
    
    /**
     * The subscribers URL
     */
    @JsonProperty("subscribers_url")
    private String subscribersUrl;
    
    /**
     * The subscription URL
     */
    @JsonProperty("subscription_url")
    private String subscriptionUrl;
    
    /**
     * The commits URL template
     */
    @JsonProperty("commits_url")
    private String commitsUrl;
    
    /**
     * The git commits URL template
     */
    @JsonProperty("git_commits_url")
    private String gitCommitsUrl;
    
    /**
     * The comments URL template
     */
    @JsonProperty("comments_url")
    private String commentsUrl;
    
    /**
     * The issue comment URL template
     */
    @JsonProperty("issue_comment_url")
    private String issueCommentUrl;
    
    /**
     * The contents URL template
     */
    @JsonProperty("contents_url")
    private String contentsUrl;
    
    /**
     * The compare URL template
     */
    @JsonProperty("compare_url")
    private String compareUrl;
    
    /**
     * The merges URL
     */
    @JsonProperty("merges_url")
    private String mergesUrl;
    
    /**
     * The archive URL template
     */
    @JsonProperty("archive_url")
    private String archiveUrl;
    
    /**
     * The downloads URL
     */
    @JsonProperty("downloads_url")
    private String downloadsUrl;
    
    /**
     * The issues URL template
     */
    @JsonProperty("issues_url")
    private String issuesUrl;
    
    /**
     * The pulls URL template
     */
    @JsonProperty("pulls_url")
    private String pullsUrl;
    
    /**
     * The milestones URL template
     */
    @JsonProperty("milestones_url")
    private String milestonesUrl;
    
    /**
     * The notifications URL template
     */
    @JsonProperty("notifications_url")
    private String notificationsUrl;
    
    /**
     * The labels URL template
     */
    @JsonProperty("labels_url")
    private String labelsUrl;
    
    /**
     * The releases URL template
     */
    @JsonProperty("releases_url")
    private String releasesUrl;
    
    /**
     * The deployments URL
     */
    @JsonProperty("deployments_url")
    private String deploymentsUrl;
    
    /**
     * When the repository was created
     */
    @JsonProperty("created_at")
    private Instant createdAt;
    
    /**
     * When the repository was last updated
     */
    @JsonProperty("updated_at")
    private Instant updatedAt;
    
    /**
     * When the repository was last pushed to
     */
    @JsonProperty("pushed_at")
    private Instant pushedAt;
    
    /**
     * The git URL of the repository
     */
    @JsonProperty("git_url")
    private String gitUrl;
    
    /**
     * The SSH URL of the repository
     */
    @JsonProperty("ssh_url")
    private String sshUrl;
    
    /**
     * The clone URL of the repository
     */
    @JsonProperty("clone_url")
    private String cloneUrl;
    
    /**
     * The SVN URL of the repository
     */
    @JsonProperty("svn_url")
    private String svnUrl;
    
    /**
     * The homepage URL of the repository
     */
    private String homepage;
    
    /**
     * The size of the repository in KB
     */
    private Integer size;
    
    /**
     * The number of stargazers
     */
    @JsonProperty("stargazers_count")
    private Integer stargazersCount;
    
    /**
     * The number of watchers
     */
    @JsonProperty("watchers_count")
    private Integer watchersCount;
    
    /**
     * The primary language of the repository
     */
    private String language;
    
    /**
     * Whether the repository has issues enabled
     */
    @JsonProperty("has_issues")
    private Boolean hasIssues;
    
    /**
     * Whether the repository has projects enabled
     */
    @JsonProperty("has_projects")
    private Boolean hasProjects;
    
    /**
     * Whether the repository has downloads enabled
     */
    @JsonProperty("has_downloads")
    private Boolean hasDownloads;
    
    /**
     * Whether the repository has wiki enabled
     */
    @JsonProperty("has_wiki")
    private Boolean hasWiki;
    
    /**
     * Whether the repository has pages enabled
     */
    @JsonProperty("has_pages")
    private Boolean hasPages;
    
    /**
     * Whether the repository has discussions enabled
     */
    @JsonProperty("has_discussions")
    private Boolean hasDiscussions;
    
    /**
     * The number of forks
     */
    @JsonProperty("forks_count")
    private Integer forksCount;
    
    /**
     * The mirror URL (if this is a mirror)
     */
    @JsonProperty("mirror_url")
    private String mirrorUrl;
    
    /**
     * Whether the repository is archived
     */
    private Boolean archived;
    
    /**
     * Whether the repository is disabled
     */
    private Boolean disabled;
    
    /**
     * The number of open issues
     */
    @JsonProperty("open_issues_count")
    private Integer openIssuesCount;
    
    /**
     * The license of the repository
     */
    private Object license;
    
    /**
     * Whether forking is allowed
     */
    @JsonProperty("allow_forking")
    private Boolean allowForking;
    
    /**
     * Whether this repository is a template
     */
    @JsonProperty("is_template")
    private Boolean isTemplate;
    
    /**
     * Whether web commit signoff is required
     */
    @JsonProperty("web_commit_signoff_required")
    private Boolean webCommitSignoffRequired;
    
    /**
     * The topics associated with the repository
     */
    private List<String> topics;
    
    /**
     * The visibility of the repository
     */
    private String visibility;
    
    /**
     * The number of forks (duplicate of forksCount)
     */
    private Integer forks;
    
    /**
     * The number of open issues (duplicate of openIssuesCount)
     */
    @JsonProperty("open_issues")
    private Integer openIssues;
    
    /**
     * The number of watchers (duplicate of watchersCount)
     */
    private Integer watchers;
    
    /**
     * The default branch of the repository
     */
    @JsonProperty("default_branch")
    private String defaultBranch;
}
