package com.redis.triage.model.feign;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model representing a GitHub label
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Label {
    
    /**
     * The unique identifier of the label
     */
    private Long id;
    
    /**
     * The node ID of the label
     */
    @JsonProperty("node_id")
    private String nodeId;
    
    /**
     * The API URL of the label
     */
    private String url;
    
    /**
     * The name of the label
     */
    private String name;
    
    /**
     * The color of the label (hex code without #)
     */
    private String color;
    
    /**
     * Whether this is a default label
     */
    @JsonProperty("default")
    private Boolean isDefault;
    
    /**
     * The description of the label
     */
    private String description;
}
