package com.redis.triage.service.chain;

import java.util.List;

interface ChainNode<T> {

    void setNext(T step);

    static <T extends ChainNode<T>> T buildChain(List<T> elements) {
        for (int i = 0; i < elements.size() - 1; i++) {
            var current = elements.get(i);
            var next = elements.get(i + 1);
            current.setNext(next);
        }
        return elements.get(0);
    }
}
