package com.redis.triage.client;

import com.redis.triage.config.feign.GitHubFeignConfig;
import com.redis.triage.model.feign.Label;
import com.redis.triage.model.webhook.GitHubIssue;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Feign client for GitHub API
 */
@FeignClient(
    name = "github-client",
    url = "https://api.github.com",
    configuration = GitHubFeignConfig.class
)
public interface GitHubFeignClient {

    /**
     * Fetches labels from a GitHub repository
     *
     * @param owner The repository owner
     * @param repo The repository name
     * @return List of labels from the repository
     */
    @GetMapping("/repos/{owner}/{repo}/labels")
    List<Label> getRepositoryLabels(@PathVariable("owner") String owner, @PathVariable("repo") String repo);

    /**
     * Fetches issues from a GitHub repository filtered by label
     *
     * @param owner The repository owner
     * @param repo The repository name
     * @param labels Comma-separated list of label names to filter by
     * @param state The state of issues to fetch (all, open, closed)
     * @param sort The sort field (created, updated, comments)
     * @param direction The sort direction (asc, desc)
     * @param perPage The number of items per page (max 100)
     * @param page The page number (1-based)
     * @return List of issues from the repository with the specified labels
     */
    @GetMapping("/repos/{owner}/{repo}/issues")
    List<GitHubIssue> getRepositoryIssuesByLabel(
        @PathVariable("owner") String owner,
        @PathVariable("repo") String repo,
        @RequestParam("labels") String labels,
        @RequestParam("state") String state,
        @RequestParam("sort") String sort,
        @RequestParam("direction") String direction,
        @RequestParam("per_page") int perPage,
        @RequestParam("page") int page
    );
}
