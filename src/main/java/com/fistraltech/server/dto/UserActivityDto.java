package com.fistraltech.server.dto;

import java.time.LocalDateTime;

/**
 * DTO for per-user activity statistics returned by the admin activity endpoint.
 *
 * <p><strong>Example JSON</strong>
 * <pre>{@code
 * {
 *   "userId": 42,
 *   "username": "alice",
 *   "email": "alice@example.com",
 *   "totalGames": 87,
 *   "wonGames": 61,
 *   "gamesLast7Days": 5,
 *   "gamesLast30Days": 18,
 *   "lastGameDate": "2026-02-28T21:00:00",
 *   "firstGameDate": "2025-11-01T09:30:00",
 *   "lastLogin": "2026-02-28T20:55:00"
 * }
 * }</pre>
 */
public class UserActivityDto {

    private Long userId;
    private String username;
    private String email;
    private long totalGames;
    private long wonGames;
    private long gamesLast7Days;
    private long gamesLast30Days;
    private LocalDateTime lastGameDate;
    private LocalDateTime firstGameDate;
    private LocalDateTime lastLogin;

    public UserActivityDto() {
    }

    // ------------------------------------------------------------------
    // Getters and Setters
    // ------------------------------------------------------------------

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public long getTotalGames() { return totalGames; }
    public void setTotalGames(long totalGames) { this.totalGames = totalGames; }

    public long getWonGames() { return wonGames; }
    public void setWonGames(long wonGames) { this.wonGames = wonGames; }

    public long getGamesLast7Days() { return gamesLast7Days; }
    public void setGamesLast7Days(long gamesLast7Days) { this.gamesLast7Days = gamesLast7Days; }

    public long getGamesLast30Days() { return gamesLast30Days; }
    public void setGamesLast30Days(long gamesLast30Days) { this.gamesLast30Days = gamesLast30Days; }

    public LocalDateTime getLastGameDate() { return lastGameDate; }
    public void setLastGameDate(LocalDateTime lastGameDate) { this.lastGameDate = lastGameDate; }

    public LocalDateTime getFirstGameDate() { return firstGameDate; }
    public void setFirstGameDate(LocalDateTime firstGameDate) { this.firstGameDate = firstGameDate; }

    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
}
