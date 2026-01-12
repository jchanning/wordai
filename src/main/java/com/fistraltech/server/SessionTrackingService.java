package com.fistraltech.server;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.fistraltech.server.model.SessionInfo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Service for tracking and managing user sessions.
 * Provides session information for admin monitoring and management.
 */
@Service
public class SessionTrackingService {
    private static final Logger logger = Logger.getLogger(SessionTrackingService.class.getName());
    
    // Map of session ID to SessionInfo
    private final Map<String, SessionInfo> activeSessions = new ConcurrentHashMap<>();
    
    /**
     * Records a new session or updates an existing one.
     * 
     * @param request HTTP request containing session and client information
     * @param userId User ID (null for anonymous users)
     * @param username Username (null for anonymous users)
     */
    public void trackSession(HttpServletRequest request, String userId, String username) {
        HttpSession httpSession = request.getSession();
        String sessionId = httpSession.getId();
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        SessionInfo sessionInfo = activeSessions.get(sessionId);
        if (sessionInfo == null) {
            sessionInfo = new SessionInfo(sessionId, userId, username, ipAddress, userAgent);
            activeSessions.put(sessionId, sessionInfo);
            logger.info("New session tracked: " + sessionId + " for user: " + username + " from IP: " + ipAddress);
        } else {
            // Update existing session
            sessionInfo.setUserId(userId);
            sessionInfo.setUsername(username);
            sessionInfo.updateActivity();
        }
    }
    
    /**
     * Updates session activity timestamp.
     * 
     * @param request HTTP request
     */
    public void updateActivity(HttpServletRequest request) {
        HttpSession httpSession = request.getSession(false);
        if (httpSession != null) {
            String sessionId = httpSession.getId();
            SessionInfo sessionInfo = activeSessions.get(sessionId);
            if (sessionInfo != null) {
                sessionInfo.updateActivity();
            }
        }
    }
    
    /**
     * Associates a game session with the user session.
     * 
     * @param request HTTP request
     * @param gameId Game session ID
     */
    public void setCurrentGame(HttpServletRequest request, String gameId) {
        HttpSession httpSession = request.getSession(false);
        if (httpSession != null) {
            String sessionId = httpSession.getId();
            SessionInfo sessionInfo = activeSessions.get(sessionId);
            if (sessionInfo != null) {
                sessionInfo.setCurrentGameId(gameId);
            }
        }
    }
    
    /**
     * Removes a session from tracking.
     * 
     * @param sessionId Session ID to remove
     */
    public void removeSession(String sessionId) {
        SessionInfo removed = activeSessions.remove(sessionId);
        if (removed != null) {
            logger.info("Session removed: " + sessionId + " for user: " + removed.getUsername());
        }
    }
    
    /**
     * Gets all active sessions.
     * 
     * @return Collection of active session information
     */
    public Collection<SessionInfo> getActiveSessions() {
        return activeSessions.values();
    }
    
    /**
     * Gets the number of active sessions.
     * 
     * @return Number of active sessions
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }
    
    /**
     * Gets session information by session ID.
     * 
     * @param sessionId Session ID
     * @return SessionInfo or null if not found
     */
    public SessionInfo getSessionInfo(String sessionId) {
        return activeSessions.get(sessionId);
    }
    
    /**
     * Cleans up inactive sessions (older than specified minutes).
     * 
     * @param inactiveMinutes Minutes of inactivity before considering session expired
     */
    public void cleanupInactiveSessions(int inactiveMinutes) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(inactiveMinutes);
        
        activeSessions.entrySet().removeIf(entry -> {
            SessionInfo sessionInfo = entry.getValue();
            boolean isInactive = sessionInfo.getLastActivity().isBefore(cutoff);
            if (isInactive) {
                logger.info("Removing inactive session: " + entry.getKey() + " for user: " + sessionInfo.getUsername());
            }
            return isInactive;
        });
    }
    
    /**
     * Extracts the real client IP address from the request.
     * Handles various proxy headers.
     * 
     * @param request HTTP request
     * @return Client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };
        
        for (String header : headerNames) {
            String ipList = request.getHeader(header);
            if (ipList != null && !ipList.isEmpty() && !"unknown".equalsIgnoreCase(ipList)) {
                // Take the first IP if multiple are present
                return ipList.split(",")[0].trim();
            }
        }
        
        return request.getRemoteAddr();
    }
}