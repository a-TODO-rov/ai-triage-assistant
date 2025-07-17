package com.redis.triage.service.chain;

import com.redis.triage.model.TriageContext;
import com.redis.triage.model.webhook.GitHubIssue;
import com.redis.triage.service.SemanticSearchService;
import com.redis.triage.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service(Constants.SEMANTIC_SEARCH_PROCESSOR_NODE)
@RequiredArgsConstructor
@Slf4j
class SemanticSearchProcessorNode extends AbstractTriageProcessorNode {

    private final SemanticSearchService semanticSearchService;

    @Override
    public void process(GitHubIssue issue, TriageContext context) {
        List<GitHubIssue> similarIssues = semanticSearchService.findSimilarIssuesAndStore(issue, context.getLabels(), 3);
        log.info("Found {} similar issues and stored new issue '{}': {}",
                similarIssues.size(), issue.getTitle(),
                similarIssues.stream().map(GitHubIssue::getTitle).toList());
        context.setSimilarIssues(similarIssues);
        nextNode.process(issue, context);
    }
}
