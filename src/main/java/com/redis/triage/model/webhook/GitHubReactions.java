package com.redis.triage.model.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model representing GitHub reactions from webhook payload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitHubReactions {
    
    /**
     * The API URL for reactions
     */
    private String url;
    
    /**
     * The total count of reactions
     */
    @JsonProperty("total_count")
    private Integer totalCount;
    
    /**
     * The number of +1 reactions
     */
    @JsonProperty("+1")
    private Integer plusOne;
    
    /**
     * The number of -1 reactions
     */
    @JsonProperty("-1")
    private Integer minusOne;
    
    /**
     * The number of laugh reactions
     */
    private Integer laugh;
    
    /**
     * The number of hooray reactions
     */
    private Integer hooray;
    
    /**
     * The number of confused reactions
     */
    private Integer confused;
    
    /**
     * The number of heart reactions
     */
    private Integer heart;
    
    /**
     * The number of rocket reactions
     */
    private Integer rocket;
    
    /**
     * The number of eyes reactions
     */
    private Integer eyes;
}
