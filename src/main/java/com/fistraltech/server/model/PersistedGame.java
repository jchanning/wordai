package com.fistraltech.server.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity representing a completed game that has been persisted to the database.
 *
 * <p>A row is written whenever a signed-in player's game ends (win or loss).
 * Guest sessions are never persisted.
 *
 * <p><strong>Storage format</strong>
 * <ul>
 *   <li>{@code guessWords}     – comma-separated guessed words, e.g. {@code "crane,stole,arise"}</li>
 *   <li>{@code guessResponses} – semicolon-separated per-letter status strings, one per guess,
 *       e.g. {@code "RAGGG;GGGGG"} where each character is G/A/R/X</li>
 * </ul>
 */
@Entity
@Table(name = "player_games")
public class PersistedGame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The numeric ID of the player (FK to the users table). */
    @Column(nullable = false)
    private Long userId;

    /** The in-memory game session UUID (informational; not a FK). */
    @Column(nullable = false, length = 50)
    private String gameId;

    @Column(nullable = false, length = 20)
    private String targetWord;

    private int wordLength;

    @Column(length = 50)
    private String dictionaryId;

    /** Comma-separated guessed words, e.g. {@code "crane,stole"}. */
    @Column(length = 500)
    private String guessWords;

    /**
     * Semicolon-separated response-code strings, one per guess.
     * Each code string has one character per letter: G, A, R, or X.
     * Example: {@code "RAGGG;GGGGG"}
     */
    @Column(length = 500)
    private String guessResponses;

    /** {@code "WON"} or {@code "LOST"}. */
    @Column(nullable = false, length = 10)
    private String result;

    private int attemptsUsed;
    private int maxAttempts;

    @Column(nullable = false)
    private LocalDateTime completedAt;

    // ------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------

    public PersistedGame() {
    }

    // ------------------------------------------------------------------
    // Getters and Setters
    // ------------------------------------------------------------------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }

    public String getTargetWord() { return targetWord; }
    public void setTargetWord(String targetWord) { this.targetWord = targetWord; }

    public int getWordLength() { return wordLength; }
    public void setWordLength(int wordLength) { this.wordLength = wordLength; }

    public String getDictionaryId() { return dictionaryId; }
    public void setDictionaryId(String dictionaryId) { this.dictionaryId = dictionaryId; }

    public String getGuessWords() { return guessWords; }
    public void setGuessWords(String guessWords) { this.guessWords = guessWords; }

    public String getGuessResponses() { return guessResponses; }
    public void setGuessResponses(String guessResponses) { this.guessResponses = guessResponses; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public int getAttemptsUsed() { return attemptsUsed; }
    public void setAttemptsUsed(int attemptsUsed) { this.attemptsUsed = attemptsUsed; }

    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
