package com.redis.triage.model.feign;

import lombok.Builder;
import lombok.Data;

/**
 * Request DTO for Slack webhook API
 */
@Data
@Builder
public class SlackWebhookRequest {
    private String text;
}
