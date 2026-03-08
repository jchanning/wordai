package com.fistraltech.server;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.fistraltech.security.model.User;
import com.fistraltech.security.repository.UserRepository;
import com.fistraltech.server.dto.GameHistoryDto;
import com.fistraltech.server.model.GameSession;

/**
 * Service that encapsulates all game-history concerns: persisting completed games
 * for authenticated players and retrieving their history.
 *
 * <p>Extracts the user-lookup + persistence logic that previously lived inline in
 * {@link com.fistraltech.server.controller.WordGameController}, so the controller
 * no longer needs to inject {@link UserRepository} or {@link PlayerGameService}
 * directly.
 *
 * <p><strong>Save policy:</strong> {@link #saveIfEnded(GameSession, Authentication, String)}
 * is a no-op only when the game has not yet ended. Authenticated callers are
 * persisted against their user row; guest sessions are persisted as anonymous
 * rows keyed by client IP.
 * Persistence failures are swallowed silently by {@link PlayerGameService#saveGame}
 * so they never break the HTTP response flow.
 */
@Service
public class GameHistoryService {

    private final PlayerGameService playerGameService;
    private final UserRepository userRepository;

    public GameHistoryService(PlayerGameService playerGameService, UserRepository userRepository) {
        this.playerGameService = playerGameService;
        this.userRepository = userRepository;
    }

    // ------------------------------------------------------------------
    // Write
    // ------------------------------------------------------------------

    /**
     * Persists the completed game for either the authenticated user or an anonymous
     * player keyed by client IP. No-op only when the game has not yet ended.
     *
     * @param session        the game session (may or may not be ended)
     * @param authentication Spring Security authentication (may be {@code null})
     * @param clientIpAddress resolved client IP address (may be {@code null})
     */
    public void saveIfEnded(GameSession session, Authentication authentication, String clientIpAddress) {
        if (!session.isGameEnded()) {
            return;
        }
        Optional<User> user = resolveUser(authentication);
        if (user.isPresent()) {
            playerGameService.saveGame(user.get().getId(), null, session, session.getDictionaryId());
            return;
        }

        playerGameService.saveGame(null, normalizeClientIpAddress(clientIpAddress), session,
                session.getDictionaryId());
    }

    // ------------------------------------------------------------------
    // Read
    // ------------------------------------------------------------------

    /**
     * Returns the game history for the authenticated principal, or empty if the
     * user cannot be resolved.
     *
     * @param authentication Spring Security authentication (may be {@code null})
     * @return optional result containing username + game history list
     */
    public Optional<UserHistory> getHistory(Authentication authentication) {
        return resolveUser(authentication).map(user -> {
            List<GameHistoryDto> games = playerGameService.getHistory(user.getId());
            return new UserHistory(user.getUsername(), games);
        });
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Resolves the Spring Security principal to a {@link User}, trying username
     * lookup first then e-mail lookup.
     *
     * @return empty if authentication is null, anonymous, or the principal string
     *         does not match any user
     */
    public Optional<User> resolveUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        String principal = authentication.getName();
        return userRepository.findByUsername(principal)
                .or(() -> userRepository.findByEmail(principal));
    }

    private String normalizeClientIpAddress(String clientIpAddress) {
        if (clientIpAddress == null || clientIpAddress.isBlank()) {
            return "Unknown IP";
        }
        return clientIpAddress.trim();
    }

    // ------------------------------------------------------------------
    // Result holder
    // ------------------------------------------------------------------

    /**
     * Lightweight carrier for a player's username and their game-history list.
     */
    public static final class UserHistory {
        private final String username;
        private final List<GameHistoryDto> games;

        public UserHistory(String username, List<GameHistoryDto> games) {
            this.username = username;
            this.games = games;
        }

        public String getUsername() { return username; }
        public List<GameHistoryDto> getGames() { return games; }
    }
}
