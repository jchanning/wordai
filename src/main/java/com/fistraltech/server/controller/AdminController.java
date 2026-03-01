package com.fistraltech.server.controller;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fistraltech.server.ActivityService;
import com.fistraltech.server.SessionTrackingService;
import com.fistraltech.server.WordGameService;
import com.fistraltech.server.dto.UserActivityDto;
import com.fistraltech.server.model.SessionInfo;

/**
 * REST controller for administrative functions.
 * 
 * <p>Provides endpoints for:
 * <ul>
 *   <li>Session monitoring and management</li>
 *   <li>System health and statistics</li>
 *   <li>User management (future)</li>
 * </ul>
 * 
 * <p>All endpoints require ADMIN role.
 */
@RestController
@RequestMapping("/api/wordai/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private static final Logger logger = Logger.getLogger(AdminController.class.getName());

    private final SessionTrackingService sessionTrackingService;
    private final WordGameService wordGameService;
    private final ActivityService activityService;

    public AdminController(SessionTrackingService sessionTrackingService,
                           WordGameService wordGameService,
                           ActivityService activityService) {
        this.sessionTrackingService = sessionTrackingService;
        this.wordGameService = wordGameService;
        this.activityService = activityService;
    }
    
    /**
     * Get all active sessions with detailed information.
     * GET /api/wordai/admin/sessions
     */
    @GetMapping("/sessions")
    public ResponseEntity<Map<String, Object>> getActiveSessions() {
        try {
            Collection<SessionInfo> sessions = sessionTrackingService.getActiveSessions();
            
            Map<String, Object> response = new HashMap<>();
            response.put("sessions", sessions);
            response.put("totalSessions", sessions.size());
            response.put("activeGameSessions", wordGameService.getActiveSessionCount());
            
            logger.info("Admin requested session information: " + sessions.size() + " active sessions");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.severe("Error retrieving session information: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve session information");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get system statistics and health information.
     * GET /api/wordai/admin/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSystemStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // Session statistics
            stats.put("activeSessions", sessionTrackingService.getActiveSessionCount());
            stats.put("activeGameSessions", wordGameService.getActiveSessionCount());
            
            // JVM statistics
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            
            Map<String, Object> memoryStats = new HashMap<>();
            memoryStats.put("totalMemory", totalMemory);
            memoryStats.put("usedMemory", usedMemory);
            memoryStats.put("freeMemory", freeMemory);
            memoryStats.put("maxMemory", maxMemory);
            memoryStats.put("usagePercent", (double) usedMemory / maxMemory * 100);
            
            stats.put("memory", memoryStats);
            stats.put("availableProcessors", runtime.availableProcessors());
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.severe("Error retrieving system stats: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve system statistics");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Clean up inactive sessions.
     * POST /api/wordai/admin/cleanup-sessions
     */
    @PostMapping("/cleanup-sessions")
    public ResponseEntity<Map<String, Object>> cleanupSessions() {
        try {
            int beforeCount = sessionTrackingService.getActiveSessionCount();
            sessionTrackingService.cleanupInactiveSessions(30); // Remove sessions inactive for 30+ minutes
            int afterCount = sessionTrackingService.getActiveSessionCount();
            
            Map<String, Object> response = new HashMap<>();
            response.put("sessionsRemoved", beforeCount - afterCount);
            response.put("remainingSessions", afterCount);
            
            logger.info("Admin cleanup removed " + (beforeCount - afterCount) + " inactive sessions");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.severe("Error during session cleanup: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to cleanup sessions");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Returns per-user activity statistics for the admin activity dashboard.
     * GET /api/wordai/admin/activity
     *
     * <p>Only users who have completed at least one game appear in the response.
     */
    @GetMapping("/activity")
    public ResponseEntity<Map<String, Object>> getUserActivity() {
        try {
            List<UserActivityDto> stats = activityService.getUserActivityStats();

            long activeUsersLast7Days  = stats.stream().filter(u -> u.getGamesLast7Days()  > 0).count();
            long activeUsersLast30Days = stats.stream().filter(u -> u.getGamesLast30Days() > 0).count();

            Map<String, Object> response = new HashMap<>();
            response.put("users",                stats);
            response.put("totalUsers",           stats.size());
            response.put("activeUsersLast7Days",  activeUsersLast7Days);
            response.put("activeUsersLast30Days", activeUsersLast30Days);

            logger.info("Admin requested activity stats: " + stats.size() + " users, "
                    + activeUsersLast7Days + " active (7d)");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.severe("Error retrieving activity stats: " + e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to retrieve activity statistics");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}