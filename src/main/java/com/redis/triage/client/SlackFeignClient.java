package com.redis.triage.client;

import com.redis.triage.model.feign.SlackWebhookRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for Slack webhook API
 */
@FeignClient(
    name = "slack-client",
    url = "${slack.webhook.url}"
)
public interface SlackFeignClient {

    /**
     * Sends a message to Slack via webhook
     *
     * @param request The webhook request
     * @return The response from Slack
     */
    @PostMapping
    String sendMessage(
        @RequestBody SlackWebhookRequest request
    );
}
