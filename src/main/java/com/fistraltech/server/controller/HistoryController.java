package com.fistraltech.server.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fistraltech.server.GameHistoryService;

/**
 * REST controller for persisted player-history resources.
 *
 * <p><strong>Base path</strong>: {@code /api/wordai/history}
 *
 * <p><strong>Primary resources</strong>
 * <ul>
 *   <li><strong>Player history</strong>: retrieve completed persisted games for the authenticated user</li>
 * </ul>
 *
 * <p><strong>Typical flow</strong>
 * <ol>
 *   <li>Authenticate</li>
 *   <li>Request history: {@code GET /api/wordai/history}</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/wordai/history")
public class HistoryController {

    private static final Logger logger = Logger.getLogger(HistoryController.class.getName());

    private final GameHistoryService gameHistoryService;

    public HistoryController(GameHistoryService gameHistoryService) {
        this.gameHistoryService = gameHistoryService;
    }

    /**
     * Get the authenticated player's persistent game history.
     * GET /api/wordai/history
     */
    @GetMapping
    public ResponseEntity<?> getPlayerHistory(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required"));
        }
        try {
            return gameHistoryService.getHistory(authentication)
                    .<ResponseEntity<?>>map(history -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("games", history.getGames());
                        response.put("total", history.getGames().size());
                        response.put("username", history.getUsername());
                        return ResponseEntity.ok(response);
                    })
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "User not found")));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error retrieving player history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve game history"));
        }
    }
}