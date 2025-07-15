package com.redis.triage.model;

import com.redis.triage.model.webhook.GitHubIssue;
import lombok.Data;

import java.util.List;

@Data
public class TriageContext {
    private List<String> labels;
    private  List<GitHubIssue> similarIssues;
    private String aiSummary;
}
