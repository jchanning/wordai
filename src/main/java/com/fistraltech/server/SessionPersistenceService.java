package com.fistraltech.server;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fistraltech.core.Response;
import com.fistraltech.server.model.ActiveGameSessionEntity;
import com.fistraltech.server.model.GameSession;
import com.fistraltech.server.repository.ActiveGameSessionRepository;

/**
 * Service that persists in-progress game sessions to the database so they survive
 * server restarts for authenticated users.
 *
 * <p>Database operations are kept separate from game logic; {@link WordGameService}
 * owns game state and calls into this service for persistence side-effects.
 *
 * <p><strong>Policy:</strong>
 * <ul>
 *   <li>Only sessions belonging to authenticated users (non-null userId) are persisted.</li>
 *   <li>Persistence failures are treated as explicit operational errors and are rethrown to callers.</li>
 * </ul>
 */
@Service
@Transactional
public class SessionPersistenceService {

    private static final Logger logger = Logger.getLogger(SessionPersistenceService.class.getName());

    private final ActiveGameSessionRepository repository;

    public SessionPersistenceService(ActiveGameSessionRepository repository) {
        this.repository = repository;
    }

    // ------------------------------------------------------------------
    // Write
    // ------------------------------------------------------------------

    /**
     * Persists a newly created game session for the given authenticated user.
     *
     * @param session the new game session (no guesses yet)
     * @param userId  the authenticated player's numeric user ID
     */
    public void save(GameSession session, Long userId) {
        try {
            ActiveGameSessionEntity entity = new ActiveGameSessionEntity();
            entity.setGameId(session.getGameId());
            entity.setUserId(userId);
            entity.setDictionaryId(session.getDictionaryId());
            entity.setBrowserSessionId(session.getBrowserSessionId() != null ? session.getBrowserSessionId() : "");
            entity.setTargetWord(session.getWordGame().getTargetWord());
            entity.setStrategy(session.getSelectedStrategy());
            entity.setGuessWords("");
            entity.setGuessResponses("");
            entity.setStatus("ACTIVE");
            LocalDateTime now = LocalDateTime.now();
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            repository.save(entity);
            logger.fine(() -> "Persisted new active session: " + session.getGameId());
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "Failed to persist new active session: " + session.getGameId(), e);
            throw new IllegalStateException("Failed to persist active session " + session.getGameId(), e);
        }
    }

    /**
     * Updates the persisted session row to reflect the latest in-memory state (guesses,
     * strategy, status).  Should be called after every guess.
     *
     * @param session the game session whose state should be written back to the DB
     */
    public void update(GameSession session) {
        try {
            repository.findById(session.getGameId()).ifPresent(entity -> {
                List<Response> guesses = session.getWordGame().getGuesses();
                entity.setGuessWords(guesses.stream()
                        .map(Response::getWord)
                        .collect(Collectors.joining(",")));
                entity.setGuessResponses(guesses.stream()
                        .map(this::encodeResponse)
                        .collect(Collectors.joining(";")));
                entity.setStrategy(session.getSelectedStrategy());
                entity.setStatus(session.isGameEnded() ? "COMPLETED" : "ACTIVE");
                entity.setUpdatedAt(LocalDateTime.now());
                repository.save(entity);
                logger.fine(() -> "Updated active session: " + session.getGameId());
            });
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "Failed to update active session: " + session.getGameId(), e);
            throw new IllegalStateException("Failed to update active session " + session.getGameId(), e);
        }
    }

    /**
     * Deletes the persisted session row.  Called when the game is explicitly removed or
     * the Caffeine cache evicts it.
     *
     * @param gameId the game session ID to delete
     */
    public void delete(String gameId) {
        try {
            repository.deleteById(gameId);
            logger.fine(() -> "Deleted active session: " + gameId);
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "Failed to delete active session: " + gameId, e);
            throw new IllegalStateException("Failed to delete active session " + gameId, e);
        }
    }

    // ------------------------------------------------------------------
    // Read
    // ------------------------------------------------------------------

    /**
     * Finds the persisted entity for the given game ID, regardless of status.
     *
     * @param gameId the game session ID
     * @return the entity, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<ActiveGameSessionEntity> findById(String gameId) {
        return repository.findById(gameId);
    }

    /**
     * Finds an ACTIVE session for a user + dictionary + browser-session combination.
     * Used to detect whether the user already has an in-progress game for the
     * requested dictionary, enabling session reconstruction on reconnect.
     *
     * @param userId       the player's numeric user ID
     * @param dictionaryId the dictionary identifier
     * @param browserSessionId the browser-window identifier stored in sessionStorage
     * @return the entity if an active session exists, or empty
     */
    @Transactional(readOnly = true)
    public Optional<ActiveGameSessionEntity> findActiveForUser(
            Long userId, String dictionaryId, String browserSessionId) {
        return repository.findByUserIdAndDictionaryIdAndBrowserSessionIdAndStatus(
                userId, dictionaryId, browserSessionId, "ACTIVE");
    }

    // ------------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------------

    /**
     * Encodes a {@link Response} as a compact status-code string (one char per letter).
     * Example: {@code "RAGGG"}.
     */
    private String encodeResponse(Response response) {
        return response.getStatuses().stream()
                .map(entry -> String.valueOf(entry.status))
                .collect(Collectors.joining());
    }
}
