package com.redis.triage.model;

public record SimilarIssueResult(String redisKey, double distance, int similarityScore) {}

