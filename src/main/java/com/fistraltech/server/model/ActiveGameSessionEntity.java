package com.fistraltech.server.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity representing an in-progress game session persisted for an authenticated user.
 *
 * <p>A row is created when a signed-in player starts a game and updated after each guess.
 * The row is deleted when the game is explicitly removed or a new game replaces it.
 * Guest sessions are never persisted here — they live only in the Caffeine cache.
 *
 * <p><strong>Storage format (consistent with {@link PersistedGame}):</strong>
 * <ul>
 *   <li>{@code guessWords}     — comma-separated guessed words, e.g. {@code "crane,stole"}</li>
 *   <li>{@code guessResponses} — semicolon-separated per-letter status strings,
 *       e.g. {@code "RAGGG;GGGGG"}</li>
 * </ul>
 */
@Entity
@Table(name = "active_game_sessions")
public class ActiveGameSessionEntity {

    /** Game session UUID — assigned by {@link com.fistraltech.server.WordGameService}. */
    @Id
    @Column(name = "game_id", length = 36, nullable = false)
    private String gameId;

    /** FK to the {@code users} table (informational; no JPA association). */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "dictionary_id", nullable = false, length = 50)
    private String dictionaryId;

    @Column(name = "browser_session_id", nullable = false, length = 100)
    private String browserSessionId = "";

    @Column(name = "target_word", nullable = false, length = 20)
    private String targetWord;

    @Column(name = "strategy", nullable = false, length = 50)
    private String strategy = "RANDOM";

    /** Comma-separated guessed words. Empty string when no guesses have been made. */
    @Column(name = "guess_words", nullable = false, length = 500)
    private String guessWords = "";

    /** Semicolon-separated response-code strings. Empty string when no guesses made. */
    @Column(name = "guess_responses", nullable = false, length = 500)
    private String guessResponses = "";

    /** {@code "ACTIVE"} while the game is in progress. */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------

    public ActiveGameSessionEntity() {
    }

    // ------------------------------------------------------------------
    // Getters and Setters
    // ------------------------------------------------------------------

    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getDictionaryId() { return dictionaryId; }
    public void setDictionaryId(String dictionaryId) { this.dictionaryId = dictionaryId; }

    public String getBrowserSessionId() { return browserSessionId; }
    public void setBrowserSessionId(String browserSessionId) { this.browserSessionId = browserSessionId; }

    public String getTargetWord() { return targetWord; }
    public void setTargetWord(String targetWord) { this.targetWord = targetWord; }

    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }

    public String getGuessWords() { return guessWords; }
    public void setGuessWords(String guessWords) { this.guessWords = guessWords; }

    public String getGuessResponses() { return guessResponses; }
    public void setGuessResponses(String guessResponses) { this.guessResponses = guessResponses; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
