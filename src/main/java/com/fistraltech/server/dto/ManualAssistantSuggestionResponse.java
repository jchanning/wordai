package com.fistraltech.server.dto;

/**
 * Response DTO for manual assistant suggestions.
 */
public class ManualAssistantSuggestionResponse {
    private String sessionId;
    private String suggestion;
    private String strategy;
    private int remainingWords;

    public ManualAssistantSuggestionResponse() {
    }

    public ManualAssistantSuggestionResponse(String sessionId,
                                             String suggestion,
                                             String strategy,
                                             int remainingWords) {
        this.sessionId = sessionId;
        this.suggestion = suggestion;
        this.strategy = strategy;
        this.remainingWords = remainingWords;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public int getRemainingWords() {
        return remainingWords;
    }

    public void setRemainingWords(int remainingWords) {
        this.remainingWords = remainingWords;
    }
}