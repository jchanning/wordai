package com.fistraltech.server.dto;

import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for consuming one Challenge Mode AI assist.
 *
 * <p><strong>Example</strong>
 * <pre>{@code
 * { "strategy": "ENTROPY" }
 * }</pre>
 */
public class ChallengeAssistRequest {
    @Pattern(regexp = ".*\\S.*", message = "strategy must not be blank")
    private String strategy;

    public ChallengeAssistRequest() {
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }
}
