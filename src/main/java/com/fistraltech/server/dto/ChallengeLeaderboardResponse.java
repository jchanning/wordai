package com.fistraltech.server.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for the Challenge Mode leaderboard endpoint.
 */
public class ChallengeLeaderboardResponse {
    private List<Entry> entries = new ArrayList<>();

    public ChallengeLeaderboardResponse() {
    }

    public ChallengeLeaderboardResponse(List<Entry> entries) {
        this.entries = entries;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    }

    public static class Entry {
        private String challengeId;
        private String username;
        private int totalScore;
        private int puzzlesCompleted;
        private String status;
        private String completedAt;

        public Entry() {
        }

        public String getChallengeId() {
            return challengeId;
        }

        public void setChallengeId(String challengeId) {
            this.challengeId = challengeId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public int getTotalScore() {
            return totalScore;
        }

        public void setTotalScore(int totalScore) {
            this.totalScore = totalScore;
        }

        public int getPuzzlesCompleted() {
            return puzzlesCompleted;
        }

        public void setPuzzlesCompleted(int puzzlesCompleted) {
            this.puzzlesCompleted = puzzlesCompleted;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getCompletedAt() {
            return completedAt;
        }

        public void setCompletedAt(String completedAt) {
            this.completedAt = completedAt;
        }
    }
}
