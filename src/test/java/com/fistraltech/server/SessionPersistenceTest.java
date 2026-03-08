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
 * updated in, and removed from the database, and that resumable sessions are
 * isolated per browser window for the same authenticated user.
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

    private static final String BROWSER_A = "browser-a";
    private static final String BROWSER_B = "browser-b";

    @Autowired
    private WordGameService wordGameService;

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

        String gameId = wordGameService.createGame(null, null, "default", userId, BROWSER_A);

        Optional<ActiveGameSessionEntity> entity = repository.findById(gameId);
        assertTrue(entity.isPresent(), "DB row must be inserted when authenticated user creates a game");
        assertEquals(userId, entity.get().getUserId(), "user_id must match");
        assertEquals("default", entity.get().getDictionaryId(), "dictionary_id must match");
        assertEquals(BROWSER_A, entity.get().getBrowserSessionId(), "browser_session_id must match");
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
        String gameId = wordGameService.createGame(null, null, "easy", userId, BROWSER_A);
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
        String gameId = wordGameService.createGame(null, null, "hard", userId, BROWSER_A);
        assertTrue(repository.findById(gameId).isPresent(), "precondition: row must exist before removal");

        wordGameService.removeGameSession(gameId);

        assertFalse(repository.findById(gameId).isPresent(), "DB row must be deleted on removeGameSession");
    }

    // -----------------------------------------------------------------------
    // T4 — createGame returns same ID when ACTIVE session already exists
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("T4: same browser session creates a new game by default for the same user and dictionary")
    void createGame_sameBrowserSession_createsDifferentGameIdByDefault() throws Exception {
        Long userId = adminUserId();

        // Create first game
        String firstGameId = wordGameService.createGame(null, null, "default", userId, BROWSER_A);
        assertTrue(repository.findById(firstGameId).isPresent(), "first game row must exist");

        // Create another game for the same user + dictionary in the same browser window
        String secondGameId = wordGameService.createGame(null, null, "default", userId, BROWSER_A);

        assertFalse(firstGameId.equals(secondGameId),
                "createGame must create a fresh game by default, even in the same browser session");

        // Two rows should exist because no implicit resumption occurs.
        List<ActiveGameSessionEntity> rows = repository.findByUserIdAndStatus(userId, "ACTIVE");
        assertEquals(2, rows.size(), "two ACTIVE sessions must exist when createGame is called twice");
    }

    @Test
    @DisplayName("T5: explicit resumeExisting returns the same ACTIVE game ID for the same browser session")
    void createGame_resumeExisting_returnsSameGameId() throws Exception {
        Long userId = adminUserId();

        String firstGameId = wordGameService.createGame(null, null, "default", userId, BROWSER_A);
        String secondGameId = wordGameService.createGame(null, null, "default", userId, BROWSER_A, true);

        assertEquals(firstGameId, secondGameId,
                "resumeExisting must reuse the existing ACTIVE game in the same browser session");

        List<ActiveGameSessionEntity> rows = repository.findByUserIdAndStatus(userId, "ACTIVE");
        assertEquals(1, rows.size(), "explicit resume should not create duplicate ACTIVE sessions");
    }

    @Test
    @DisplayName("T6: different browser sessions create independent ACTIVE games for the same user and dictionary")
    void createGame_differentBrowserSessions_returnDifferentGameIds() throws Exception {
        Long userId = adminUserId();

        String firstGameId = wordGameService.createGame(null, null, "default", userId, BROWSER_A);
        String secondGameId = wordGameService.createGame(null, null, "default", userId, BROWSER_B);

        assertFalse(firstGameId.equals(secondGameId),
                "different browser sessions must not share the same active game");

        List<ActiveGameSessionEntity> rows = repository.findByUserIdAndStatus(userId, "ACTIVE");
        assertEquals(2, rows.size(), "two ACTIVE sessions should exist for two browser sessions");
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
