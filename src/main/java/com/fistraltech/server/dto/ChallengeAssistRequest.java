package com.fistraltech.server.dto;

/**
 * Request DTO for consuming one Challenge Mode AI assist.
 *
 * <p><strong>Example</strong>
 * <pre>{@code
 * { "strategy": "ENTROPY" }
 * }</pre>
 */
public class ChallengeAssistRequest {
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
