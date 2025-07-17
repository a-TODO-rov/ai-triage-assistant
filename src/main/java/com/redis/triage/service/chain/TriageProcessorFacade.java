package com.redis.triage.service.chain;

import com.redis.triage.model.TriageContext;
import com.redis.triage.model.webhook.GitHubIssue;
import com.redis.triage.util.Constants;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TriageProcessorFacade {

    private final Map<String, TriageProcessorNode> processors;
    private TriageProcessorNode chain;

    @PostConstruct
    public void init() {

        chain = ChainNode.buildChain(List.of(
                processors.get(Constants.LABELING_PROCESSOR_NODE),
                processors.get(Constants.SEMANTIC_SEARCH_PROCESSOR_NODE),
                processors.get(Constants.AI_SUMMARY_PROCESSOR_NODE),
                processors.get(Constants.SLACK_NOTIFICATION_NODE)
        ));
    }

    public void process(GitHubIssue issue) {
        chain.process(issue, new TriageContext());
    }
}
