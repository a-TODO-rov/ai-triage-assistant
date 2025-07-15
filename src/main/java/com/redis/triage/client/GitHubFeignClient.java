package com.redis.triage.client;

import com.redis.triage.config.feign.GitHubFeignConfig;
import com.redis.triage.model.feign.Label;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

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
}
