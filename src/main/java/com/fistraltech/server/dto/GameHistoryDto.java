package com.fistraltech.server.dto;

import java.util.List;

/**
 * DTO returned by {@code GET /api/wordai/history} representing one completed game
 * retrieved from persistent storage.
 */
public class GameHistoryDto {

    private Long id;
    private String targetWord;
    private int wordLength;
    private String dictionaryId;

    /** Ordered list of words guessed, e.g. {@code ["crane", "stole"]}. */
    private List<String> guesses;

    /**
     * Ordered list of per-guess response-code strings, one character per letter.
     * Characters: G (green), A (amber), R (red/absent), X (excess).
     * Example: {@code ["RAGGG", "GGGGG"]}.
     */
    private List<String> responses;

    /** {@code "WON"} or {@code "LOST"}. */
    private String result;

    private int attemptsUsed;
    private int maxAttempts;

    /** ISO-8601 timestamp string, e.g. {@code "2025-02-27T14:30:00"}. */
    private String completedAt;

    // ------------------------------------------------------------------
    // Getters and Setters
    // ------------------------------------------------------------------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTargetWord() { return targetWord; }
    public void setTargetWord(String targetWord) { this.targetWord = targetWord; }

    public int getWordLength() { return wordLength; }
    public void setWordLength(int wordLength) { this.wordLength = wordLength; }

    public String getDictionaryId() { return dictionaryId; }
    public void setDictionaryId(String dictionaryId) { this.dictionaryId = dictionaryId; }

    public List<String> getGuesses() { return guesses; }
    public void setGuesses(List<String> guesses) { this.guesses = guesses; }

    public List<String> getResponses() { return responses; }
    public void setResponses(List<String> responses) { this.responses = responses; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public int getAttemptsUsed() { return attemptsUsed; }
    public void setAttemptsUsed(int attemptsUsed) { this.attemptsUsed = attemptsUsed; }

    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

    public String getCompletedAt() { return completedAt; }
    public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }
}
