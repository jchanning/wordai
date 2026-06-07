package com.fistraltech.server.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for changing the strategy used by a manual assistant session.
 */
public class ManualAssistantStrategyRequest {
    @NotBlank(message = "Strategy is required")
    private String strategy;

    public ManualAssistantStrategyRequest() {
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }
}