package com.redis.triage.service.chain;

import com.redis.triage.model.TriageContext;
import com.redis.triage.model.webhook.GitHubIssue;
import com.redis.triage.service.SlackNotifier;
import com.redis.triage.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service(Constants.SLACK_NOTIFICATION_NODE)
@RequiredArgsConstructor
@Slf4j
class SlackNotificationNode extends AbstractTriageProcessorNode {

    private final SlackNotifier slackNotifier;

    @Override
    public void process(GitHubIssue issue, TriageContext context) {
        slackNotifier.sendNotification(issue, context.getLabels(), context.getSimilarIssues(), context.getAiSummary());
    }
}
