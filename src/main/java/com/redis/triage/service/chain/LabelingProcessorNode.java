package com.redis.triage.service.chain;

import com.redis.triage.model.TriageContext;
import com.redis.triage.model.webhook.GitHubIssue;
import com.redis.triage.service.LabelingService;
import com.redis.triage.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service(Constants.LABELING_PROCESSOR_NODE)
@RequiredArgsConstructor
@Slf4j
class LabelingProcessorNode extends AbstractTriageProcessorNode {

    private final LabelingService labelingService;

    @Override
    public void process(GitHubIssue issue, TriageContext context) {

        String repositoryUrl = issue.getRepositoryUrl();
        List<String> labels = labelingService.generateLabels(issue, repositoryUrl);
        log.info("Generated labels for issue '{}': {}", issue.getTitle(), labels);
        context.setLabels(labels);
        nextNode.process(issue, context);
    }
}
