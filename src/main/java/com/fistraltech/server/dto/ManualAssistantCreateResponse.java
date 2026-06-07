package com.fistraltech.server.dto;

/**
 * Response DTO for manual assistant session creation.
 */
public class ManualAssistantCreateResponse {
    private String sessionId;
    private String dictionaryId;
    private int wordLength;
    private String strategy;
    private int remainingWords;

    public ManualAssistantCreateResponse() {
    }

    public ManualAssistantCreateResponse(String sessionId,
                                         String dictionaryId,
                                         int wordLength,
                                         String strategy,
                                         int remainingWords) {
        this.sessionId = sessionId;
        this.dictionaryId = dictionaryId;
        this.wordLength = wordLength;
        this.strategy = strategy;
        this.remainingWords = remainingWords;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getDictionaryId() {
        return dictionaryId;
    }

    public void setDictionaryId(String dictionaryId) {
        this.dictionaryId = dictionaryId;
    }

    public int getWordLength() {
        return wordLength;
    }

    public void setWordLength(int wordLength) {
        this.wordLength = wordLength;
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