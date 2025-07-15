package com.redis.triage.chain;

import com.redis.triage.model.TriageContext;
import com.redis.triage.model.webhook.GitHubIssue;

interface TriageProcessorNode extends ChainNode<TriageProcessorNode> {
    void process(GitHubIssue issue, TriageContext context);
}
