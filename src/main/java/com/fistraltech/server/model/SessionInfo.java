package com.fistraltech.server.model;

import java.time.LocalDateTime;

/**
 * Represents information about an active user session.
 * Used for admin panel session monitoring and management.
 */
public class SessionInfo {
    private String sessionId;
    private String userId;
    private String username;
    private String ipAddress;
    private String userAgent;
    private String browser;
    private String operatingSystem;
    private LocalDateTime loginTime;
    private LocalDateTime lastActivity;
    private boolean active;
    private String currentGameId;
    
    public SessionInfo() {
        this.loginTime = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
        this.active = true;
    }
    
    public SessionInfo(String sessionId, String userId, String username, String ipAddress, String userAgent) {
        this();
        this.sessionId = sessionId;
        this.userId = userId;
        this.username = username;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        parseUserAgent(userAgent);
    }
    
    private void parseUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            this.browser = "Unknown";
            this.operatingSystem = "Unknown";
            return;
        }
        
        // Parse browser
        if (userAgent.contains("Chrome")) {
            this.browser = "Chrome";
        } else if (userAgent.contains("Firefox")) {
            this.browser = "Firefox";
        } else if (userAgent.contains("Safari") && !userAgent.contains("Chrome")) {
            this.browser = "Safari";
        } else if (userAgent.contains("Edge")) {
            this.browser = "Edge";
        } else if (userAgent.contains("Opera")) {
            this.browser = "Opera";
        } else {
            this.browser = "Unknown";
        }
        
        // Parse OS
        if (userAgent.contains("Windows")) {
            this.operatingSystem = "Windows";
        } else if (userAgent.contains("Mac OS")) {
            this.operatingSystem = "macOS";
        } else if (userAgent.contains("Linux")) {
            this.operatingSystem = "Linux";
        } else if (userAgent.contains("Android")) {
            this.operatingSystem = "Android";
        } else if (userAgent.contains("iPhone") || userAgent.contains("iPad")) {
            this.operatingSystem = "iOS";
        } else {
            this.operatingSystem = "Unknown";
        }
    }
    
    public void updateActivity() {
        this.lastActivity = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
        parseUserAgent(userAgent);
    }
    
    public String getBrowser() {
        return browser;
    }
    
    public void setBrowser(String browser) {
        this.browser = browser;
    }
    
    public String getOperatingSystem() {
        return operatingSystem;
    }
    
    public void setOperatingSystem(String operatingSystem) {
        this.operatingSystem = operatingSystem;
    }
    
    public LocalDateTime getLoginTime() {
        return loginTime;
    }
    
    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }
    
    public LocalDateTime getLastActivity() {
        return lastActivity;
    }
    
    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public String getCurrentGameId() {
        return currentGameId;
    }
    
    public void setCurrentGameId(String currentGameId) {
        this.currentGameId = currentGameId;
    }
}