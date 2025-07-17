package com.redis.triage.service.chain;

import com.redis.triage.model.TriageContext;
import com.redis.triage.model.webhook.GitHubIssue;
import com.redis.triage.service.AISummaryService;
import com.redis.triage.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service(Constants.AI_SUMMARY_PROCESSOR_NODE)
@RequiredArgsConstructor
@Slf4j
class AiSummeryProcessorNode extends AbstractTriageProcessorNode {

    private final AISummaryService aiSummaryService;

    @Override
    public void process(GitHubIssue issue, TriageContext context) {
        String aiSummary = aiSummaryService.generateSummaryWithContext(issue, context.getLabels(), context.getSimilarIssues());
        log.info("Generated AI summary for issue '{}': {}", issue.getTitle(), aiSummary);
        context.setAiSummary(aiSummary);
        nextNode.process(issue, context);
    }
}
