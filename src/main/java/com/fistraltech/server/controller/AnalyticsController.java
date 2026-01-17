package com.fistraltech.server.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fistraltech.server.WordGameService;

/**
 * REST controller for premium analytics and features.
 * 
 * <p>Provides endpoints for:
 * <ul>
 *   <li>Advanced analytics and statistics</li>
 *   <li>Game data export</li>
 *   <li>Historical performance analysis</li>
 *   <li>Premium algorithm features</li>
 * </ul>
 * 
 * <p>Requires PREMIUM or ADMIN role.
 */
@RestController
@RequestMapping("/api/wordai/analytics")
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyRole('PREMIUM', 'ADMIN')")
public class AnalyticsController {
    private static final Logger logger = Logger.getLogger(AnalyticsController.class.getName());
    
    @Autowired
    private WordGameService wordGameService;
    
    /**
     * Get advanced game statistics for a specific game.
     * GET /api/wordai/analytics/games/{gameId}/stats
     */
    @GetMapping("/games/{gameId}/stats")
    public ResponseEntity<Map<String, Object>> getGameAnalytics(@PathVariable String gameId) {
        try {
            // This would integrate with existing GameAnalytics classes
            Map<String, Object> analytics = new HashMap<>();
            analytics.put("gameId", gameId);
            analytics.put("feature", "Advanced Statistics");
            analytics.put("note", "Premium feature - detailed game analysis");
            analytics.put("available", "Coming soon");
            
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            logger.severe("Failed to get game analytics for game " + gameId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve analytics"));
        }
    }
    
    /**
     * Export game data in various formats.
     * POST /api/wordai/analytics/export
     */
    @PostMapping("/export")
    public ResponseEntity<Map<String, Object>> exportGameData(@RequestBody Map<String, Object> exportRequest) {
        try {
            String format = (String) exportRequest.getOrDefault("format", "csv");
            String gameId = (String) exportRequest.get("gameId");
            
            Map<String, Object> response = new HashMap<>();
            response.put("feature", "Data Export");
            response.put("format", format);
            response.put("gameId", gameId);
            response.put("note", "Premium feature - export game data");
            response.put("available", "Coming soon");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.severe("Failed to export game data: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to export data"));
        }
    }
    
    /**
     * Get global statistics across all games.
     * GET /api/wordai/analytics/global
     */
    @GetMapping("/global")
    public ResponseEntity<Map<String, Object>> getGlobalAnalytics() {
        try {
            Map<String, Object> analytics = new HashMap<>();
            analytics.put("totalGames", wordGameService.getActiveSessionCount());
            analytics.put("feature", "Global Analytics");
            analytics.put("note", "Premium feature - cross-game analysis");
            analytics.put("available", "Coming soon");
            
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            logger.severe("Failed to get global analytics: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve global analytics"));
        }
    }
    
    /**
     * Get performance trends over time.
     * GET /api/wordai/analytics/trends
     */
    @GetMapping("/trends")
    public ResponseEntity<Map<String, Object>> getPerformanceTrends() {
        try {
            Map<String, Object> trends = new HashMap<>();
            trends.put("feature", "Performance Trends");
            trends.put("note", "Premium feature - historical performance analysis");
            trends.put("available", "Coming soon");
            
            return ResponseEntity.ok(trends);
        } catch (Exception e) {
            logger.severe("Failed to get performance trends: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve trends"));
        }
    }
}