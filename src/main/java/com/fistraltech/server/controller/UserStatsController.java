package com.fistraltech.server.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fistraltech.server.WordGameService;

/**
 * REST controller for user statistics and basic features.
 * 
 * <p>Provides endpoints for:
 * <ul>
 *   <li>Basic user statistics</li>
 *   <li>Game history</li>
 *   <li>Personal performance metrics</li>
 * </ul>
 * 
 * <p>Requires authentication (any registered user).
 */
@RestController
@RequestMapping("/api/wordai/stats")
@CrossOrigin(origins = "*")
@PreAuthorize("isAuthenticated()")
public class UserStatsController {
    private static final Logger logger = Logger.getLogger(UserStatsController.class.getName());
    
    @Autowired
    private WordGameService wordGameService;
    
    /**
     * Get basic user statistics.
     * GET /api/wordai/stats/user
     */
    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> getUserStats(Authentication authentication) {
        try {
            String username = authentication.getName();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("username", username);
            stats.put("gamesPlayed", 0); // Would be tracked in database
            stats.put("averageGuesses", 0.0);
            stats.put("winRate", 0.0);
            stats.put("currentStreak", 0);
            stats.put("maxStreak", 0);
            stats.put("feature", "Basic User Statistics");
            stats.put("note", "Available to all registered users");
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.severe("Failed to get user stats: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve user statistics"));
        }
    }
    
    /**
     * Get basic game history for the user.
     * GET /api/wordai/stats/history
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getUserHistory(Authentication authentication) {
        try {
            String username = authentication.getName();
            
            Map<String, Object> history = new HashMap<>();
            history.put("username", username);
            history.put("recentGames", new Object[0]); // Would be populated from database
            history.put("feature", "Game History");
            history.put("note", "Available to all registered users");
            history.put("totalGames", 0);
            
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            logger.severe("Failed to get user history: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve user history"));
        }
    }
    
    /**
     * Get statistics for a specific game (if owned by user).
     * GET /api/wordai/stats/games/{gameId}
     */
    @GetMapping("/games/{gameId}")
    public ResponseEntity<Map<String, Object>> getGameStats(@PathVariable String gameId, Authentication authentication) {
        try {
            String username = authentication.getName();
            
            // In a real implementation, you'd check if the user owns this game
            Map<String, Object> gameStats = new HashMap<>();
            gameStats.put("gameId", gameId);
            gameStats.put("username", username);
            gameStats.put("guesses", 0);
            gameStats.put("duration", "00:00:00");
            gameStats.put("completed", false);
            gameStats.put("feature", "Game Statistics");
            gameStats.put("note", "Basic stats for registered users");
            
            return ResponseEntity.ok(gameStats);
        } catch (Exception e) {
            logger.severe("Failed to get game stats for game " + gameId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve game statistics"));
        }
    }
}