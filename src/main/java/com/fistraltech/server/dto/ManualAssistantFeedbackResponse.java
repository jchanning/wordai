package com.fistraltech.server.dto;

/**
 * Response DTO returned after manual Wordle feedback is applied.
 */
public class ManualAssistantFeedbackResponse {
    private String sessionId;
    private String guessedWord;
    private String normalizedFeedback;
    private int attemptNumber;
    private int remainingWords;
    private boolean winnerPattern;

    public ManualAssistantFeedbackResponse() {
    }

    public ManualAssistantFeedbackResponse(String sessionId,
                                           String guessedWord,
                                           String normalizedFeedback,
                                           int attemptNumber,
                                           int remainingWords,
                                           boolean winnerPattern) {
        this.sessionId = sessionId;
        this.guessedWord = guessedWord;
        this.normalizedFeedback = normalizedFeedback;
        this.attemptNumber = attemptNumber;
        this.remainingWords = remainingWords;
        this.winnerPattern = winnerPattern;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getGuessedWord() {
        return guessedWord;
    }

    public void setGuessedWord(String guessedWord) {
        this.guessedWord = guessedWord;
    }

    public String getNormalizedFeedback() {
        return normalizedFeedback;
    }

    public void setNormalizedFeedback(String normalizedFeedback) {
        this.normalizedFeedback = normalizedFeedback;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(int attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public int getRemainingWords() {
        return remainingWords;
    }

    public void setRemainingWords(int remainingWords) {
        this.remainingWords = remainingWords;
    }

    public boolean isWinnerPattern() {
        return winnerPattern;
    }

    public void setWinnerPattern(boolean winnerPattern) {
        this.winnerPattern = winnerPattern;
    }
}