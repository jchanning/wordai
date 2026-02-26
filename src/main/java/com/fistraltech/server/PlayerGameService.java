package com.fistraltech.server;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fistraltech.core.Response;
import com.fistraltech.server.dto.GameHistoryDto;
import com.fistraltech.server.model.GameSession;
import com.fistraltech.server.model.PersistedGame;
import com.fistraltech.server.repository.PlayerGameRepository;

/**
 * Service responsible for persisting completed games and retrieving a player's game history.
 *
 * <p>A game is saved whenever a signed-in player's game reaches a terminal state (won or lost).
 * Guest sessions are never saved.
 *
 * <p>The history is capped at the 100 most-recent games per player on retrieval to
 * keep response payloads manageable.
 */
@Service
@Transactional
public class PlayerGameService {

    private static final Logger logger = Logger.getLogger(PlayerGameService.class.getName());
    private static final int MAX_HISTORY = 100;

    private final PlayerGameRepository repository;

    public PlayerGameService(PlayerGameRepository repository) {
        this.repository = repository;
    }

    // ------------------------------------------------------------------
    // Write
    // ------------------------------------------------------------------

    /**
     * Persists a completed game for a signed-in player.
     *
     * <p>Silently swallows exceptions so that a persistence failure never breaks
     * the game response flow.
     *
     * @param userId       the player's numeric user ID from the {@code users} table
     * @param session      the completed (gameEnded == true) game session
     * @param dictionaryId the dictionary identifier used when the game was created
     */
    public void saveGame(Long userId, GameSession session, String dictionaryId) {
        try {
            List<Response> guesses = session.getWordGame().getGuesses();

            String guessWords = guesses.stream()
                    .map(Response::getWord)
                    .collect(Collectors.joining(","));

            String guessResponses = guesses.stream()
                    .map(this::encodeResponse)
                    .collect(Collectors.joining(";"));

            boolean won = !guesses.isEmpty() && guesses.get(guesses.size() - 1).getWinner();

            PersistedGame pg = new PersistedGame();
            pg.setUserId(userId);
            pg.setGameId(session.getGameId());
            pg.setTargetWord(session.getWordGame().getTargetWord());
            pg.setWordLength(session.getWordGame().getDictionary().getWordLength());
            pg.setDictionaryId(dictionaryId != null ? dictionaryId : "default");
            pg.setGuessWords(guessWords.isEmpty() ? null : guessWords);
            pg.setGuessResponses(guessResponses.isEmpty() ? null : guessResponses);
            pg.setResult(won ? "WON" : "LOST");
            pg.setAttemptsUsed(session.getCurrentAttempts());
            pg.setMaxAttempts(session.getMaxAttempts());
            pg.setCompletedAt(LocalDateTime.now());

            repository.save(pg);
            logger.info(() -> "Saved game for user " + userId + ": " + pg.getResult()
                    + " target=" + pg.getTargetWord() + " attempts=" + pg.getAttemptsUsed());
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to persist completed game for user " + userId, e);
        }
    }

    // ------------------------------------------------------------------
    // Read
    // ------------------------------------------------------------------

    /**
     * Returns up to {@value #MAX_HISTORY} most-recent completed games for a player.
     *
     * @param userId the player's numeric user ID
     * @return list of DTOs ordered newest-first
     */
    @Transactional(readOnly = true)
    public List<GameHistoryDto> getHistory(Long userId) {
        List<PersistedGame> games = repository.findByUserIdOrderByCompletedAtDesc(userId);
        if (games.size() > MAX_HISTORY) {
            games = games.subList(0, MAX_HISTORY);
        }
        return games.stream().map(this::toDto).collect(Collectors.toList());
    }

    /** Total persisted games for a player (for stats displays). */
    @Transactional(readOnly = true)
    public long countGames(Long userId) {
        return repository.countByUserId(userId);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Converts a {@link Response} to a compact status-code string, one char per letter.
     * Example: {@code "RAGGG"}.
     */
    private String encodeResponse(Response response) {
        return response.getStatuses().stream()
                .map(entry -> String.valueOf(entry.status))
                .collect(Collectors.joining());
    }

    private GameHistoryDto toDto(PersistedGame pg) {
        GameHistoryDto dto = new GameHistoryDto();
        dto.setId(pg.getId());
        dto.setTargetWord(pg.getTargetWord());
        dto.setWordLength(pg.getWordLength());
        dto.setDictionaryId(pg.getDictionaryId());

        dto.setGuesses(splitOrEmpty(pg.getGuessWords(), ","));
        dto.setResponses(splitOrEmpty(pg.getGuessResponses(), ";"));

        dto.setResult(pg.getResult());
        dto.setAttemptsUsed(pg.getAttemptsUsed());
        dto.setMaxAttempts(pg.getMaxAttempts());
        dto.setCompletedAt(pg.getCompletedAt() != null ? pg.getCompletedAt().toString() : null);
        return dto;
    }

    private List<String> splitOrEmpty(String value, String delimiter) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(value.split(delimiter));
    }
}
