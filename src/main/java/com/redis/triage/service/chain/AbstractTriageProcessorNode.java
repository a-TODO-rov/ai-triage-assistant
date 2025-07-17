package com.redis.triage.service.chain;

abstract class AbstractTriageProcessorNode implements TriageProcessorNode {

    protected TriageProcessorNode nextNode;

    @Override
    public void setNext(TriageProcessorNode nextNode) {
        this.nextNode = nextNode;
    }
}
