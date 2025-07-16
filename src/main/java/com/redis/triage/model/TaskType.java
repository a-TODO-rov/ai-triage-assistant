package com.redis.triage.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum TaskType {

    LABELING("labeling"),
    SUMMARIZATION("summarization"),
    UNKNOWN("unknown"),
    ;

    private final String type;
}
