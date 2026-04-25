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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fistraltech.server.ActivityService;
import com.fistraltech.server.AlgorithmFeatureService;
import com.fistraltech.server.SessionTrackingService;
import com.fistraltech.server.WordGameService;
import com.fistraltech.server.dto.AlgorithmPolicyResponse;
import com.fistraltech.server.dto.UpdateAlgorithmPolicyRequest;
import com.fistraltech.server.dto.UserActivityDto;
import com.fistraltech.server.model.SessionInfo;
import com.fistraltech.web.ApiErrors;

import jakarta.validation.Valid;

/**
 * REST controller for administrative functions.
 * 
 * <p>Provides endpoints for:
 * <ul>
 *   <li>Session monitoring and management</li>
 *   <li>System health and statistics</li>
 *   <li>Runtime algorithm policy updates</li>
 *   <li>User management (future)</li>
 * </ul>
 * 
 * <p>All endpoints require ADMIN role.
 */
@RestController
@RequestMapping({ApiRoutes.LEGACY_ROOT + "/admin", ApiRoutes.V1_ROOT + "/admin"})
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private static final Logger logger = Logger.getLogger(AdminController.class.getName());

    private final SessionTrackingService sessionTrackingService;
    private final WordGameService wordGameService;
    private final ActivityService activityService;
    private final AlgorithmFeatureService algorithmFeatureService;

    public AdminController(SessionTrackingService sessionTrackingService,
                           WordGameService wordGameService,
                           ActivityService activityService,
                           AlgorithmFeatureService algorithmFeatureService) {
        this.sessionTrackingService = sessionTrackingService;
        this.wordGameService = wordGameService;
        this.activityService = activityService;
        this.algorithmFeatureService = algorithmFeatureService;
    }
    
    /**
     * Get all active sessions with detailed information.
     * GET /api/wordai/admin/sessions
     */
    @GetMapping("/sessions")
    public ResponseEntity<?> getActiveSessions() {
        try {
            Collection<SessionInfo> sessions = sessionTrackingService.getActiveSessions();
            
            Map<String, Object> response = new HashMap<>();
            response.put("sessions", sessions);
            response.put("totalSessions", sessions.size());
            response.put("activeGameSessions", wordGameService.getActiveSessionCount());
            
            logger.info("Admin requested session information: " + sessions.size() + " active sessions");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.log(java.util.logging.Level.SEVERE, "Error retrieving session information", e);
            return ApiErrors.response(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to retrieve session information", e.getMessage());
        }
    }
    
    /**
     * Get system statistics and health information.
     * GET /api/wordai/admin/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getSystemStats() {
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
            logger.log(java.util.logging.Level.SEVERE, "Error retrieving system stats", e);
            return ApiErrors.response(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to retrieve system statistics", e.getMessage());
        }
    }
    
    /**
     * Clean up inactive sessions.
     * POST /api/wordai/admin/cleanup-sessions
     */
    @PostMapping("/cleanup-sessions")
    public ResponseEntity<?> cleanupSessions() {
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
            logger.log(java.util.logging.Level.SEVERE, "Error during session cleanup", e);
            return ApiErrors.response(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to cleanup sessions", e.getMessage());
        }
    }

    /**
     * Returns per-user activity statistics for the admin activity dashboard.
     * GET /api/wordai/admin/activity
     *
        * <p>Every player who has completed at least one game appears in the response,
        * including anonymous players grouped by client IP.
     */
    @GetMapping("/activity")
    public ResponseEntity<?> getUserActivity() {
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
            logger.log(java.util.logging.Level.SEVERE, "Error retrieving activity stats", e);
            return ApiErrors.response(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to retrieve activity statistics", e.getMessage());
        }
    }

    /**
     * Updates the runtime enabled state for a registered algorithm.
     * PUT /api/wordai/admin/algorithms/{algorithmId}
     */
    @PutMapping("/algorithms/{algorithmId}")
    public ResponseEntity<?> updateAlgorithmPolicy(@PathVariable String algorithmId,
                                                   @Valid @RequestBody UpdateAlgorithmPolicyRequest request) {
        try {
            AlgorithmPolicyResponse response = AlgorithmPolicyResponse.from(
                    algorithmFeatureService.updateAlgorithmEnabled(algorithmId, request.getEnabled()));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ApiErrors.response(HttpStatus.BAD_REQUEST, "Invalid algorithm", e.getMessage());
        } catch (Exception e) {
            logger.log(java.util.logging.Level.SEVERE,
                    "Error updating algorithm policy for " + algorithmId, e);
            return ApiErrors.response(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to update algorithm policy", e.getMessage());
        }
    }
}