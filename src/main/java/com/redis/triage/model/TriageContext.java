package com.redis.triage.model;

import lombok.Data;

import java.util.List;

@Data
public class TriageContext {
    private List<String> labels;
    private  List<GitHubSimilarIssue> similarIssues;
    private String aiSummary;
}
