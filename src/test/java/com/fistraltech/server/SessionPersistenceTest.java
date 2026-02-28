package com.fistraltech.server;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.fistraltech.security.repository.UserRepository;
import com.fistraltech.server.model.ActiveGameSessionEntity;
import com.fistraltech.server.model.GameSession;
import com.fistraltech.server.repository.ActiveGameSessionRepository;

/**
 * Integration tests for {@link SessionPersistenceService} and the persistence
 * hooks in {@link WordGameService}.
 *
 * <p>Verifies that authenticated-user game sessions are correctly saved to,
 * updated in, and removed from the database, and that the same session is
 * returned when a user already has an ACTIVE game for a given dictionary.
 *
 * <p>Uses a dedicated in-memory H2 database + Flyway so these tests never touch
 * the dev file-based database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:persistence_test_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.h2.console.enabled=false"
})
@DisplayName("SessionPersistenceTest")
class SessionPersistenceTest {

    @Autowired
    private WordGameService wordGameService;

    @Autowired
    private SessionPersistenceService sessionPersistenceService;

    @Autowired
    private ActiveGameSessionRepository repository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Evict any leftover ACTIVE sessions for the admin user before each test
     * so tests are independent.
     */
    @BeforeEach
    void cleanupActiveSessions() {
        Long userId = adminUserId();
        if (userId != null) {
            List<ActiveGameSessionEntity> active = repository.findByUserIdAndStatus(userId, "ACTIVE");
            active.forEach(e -> wordGameService.removeGameSession(e.getGameId()));
        }
    }

    // -----------------------------------------------------------------------
    // T1 — create game → DB row inserted
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("T1: authenticated user creates game → row inserted in DB with ACTIVE status")
    void createGame_authenticated_rowInsertedInDB() throws Exception {
        Long userId = adminUserId();

        String gameId = wordGameService.createGame(null, null, "default", userId);

        Optional<ActiveGameSessionEntity> entity = repository.findById(gameId);
        assertTrue(entity.isPresent(), "DB row must be inserted when authenticated user creates a game");
        assertEquals(userId, entity.get().getUserId(), "user_id must match");
        assertEquals("default", entity.get().getDictionaryId(), "dictionary_id must match");
        assertEquals("ACTIVE", entity.get().getStatus(), "status must be ACTIVE");
        assertEquals("", entity.get().getGuessWords(), "guess_words must be empty before any guess");
        assertNotNull(entity.get().getTargetWord(), "target_word must not be null");
    }

    // -----------------------------------------------------------------------
    // T2 — after guess → DB row updated
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("T2: after a guess, DB row updated with guess word and COMPLETED status on win")
    void makeGuess_authenticated_rowUpdatedInDB() throws Exception {
        Long userId = adminUserId();
        String gameId = wordGameService.createGame(null, null, "easy", userId);
        GameSession session = wordGameService.getGameSession(gameId);

        // Guess the target word to win immediately
        String target = session.getWordGame().getTargetWord();
        wordGameService.makeGuess(gameId, target);

        Optional<ActiveGameSessionEntity> entity = repository.findById(gameId);
        assertTrue(entity.isPresent(), "DB row must still exist after guess");
        assertFalse(entity.get().getGuessWords().isEmpty(), "guess_words must be populated after a guess");
        assertTrue(entity.get().getGuessWords().contains(target),
                "guess_words must contain the guessed word");
        assertEquals("COMPLETED", entity.get().getStatus(), "status must be COMPLETED after a win");
    }

    // -----------------------------------------------------------------------
    // T3 — remove game → DB row deleted
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("T3: remove game → DB row deleted")
    void removeGame_authenticated_rowDeletedFromDB() throws Exception {
        Long userId = adminUserId();
        String gameId = wordGameService.createGame(null, null, "hard", userId);
        assertTrue(repository.findById(gameId).isPresent(), "precondition: row must exist before removal");

        wordGameService.removeGameSession(gameId);

        assertFalse(repository.findById(gameId).isPresent(), "DB row must be deleted on removeGameSession");
    }

    // -----------------------------------------------------------------------
    // T4 — createGame returns same ID when ACTIVE session already exists
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("T4: createGame returns existing ACTIVE game ID for same user + dictionary (no duplicate rows)")
    void createGame_existingActiveSession_returnsSameGameId() throws Exception {
        Long userId = adminUserId();

        // Create first game
        String firstGameId = wordGameService.createGame(null, null, "default", userId);
        assertTrue(repository.findById(firstGameId).isPresent(), "first game row must exist");

        // Create another game for the same user + dictionary
        String secondGameId = wordGameService.createGame(null, null, "default", userId);

        assertEquals(firstGameId, secondGameId,
                "createGame must return the existing ACTIVE game ID, not create a duplicate");

        // Only one row should exist for this user + dictionary
        List<ActiveGameSessionEntity> rows = repository.findByUserIdAndStatus(userId, "ACTIVE");
        assertEquals(1, rows.size(), "exactly one ACTIVE session must exist per user+dictionary");
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private Long adminUserId() {
        return userRepository.findByUsername("admin")
                .map(u -> u.getId())
                .orElse(null);
    }
}
