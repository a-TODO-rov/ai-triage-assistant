package com.redis.triage.model;

import com.redis.triage.model.webhook.GitHubIssue;

public record GitHubSimilarIssue(GitHubIssue issue, double distance, int similarityScore) {}

