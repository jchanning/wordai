package com.fistraltech.server.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Persisted summary of a completed Challenge Mode run for leaderboard queries.
 */
@Entity
@Table(name = "challenge_results")
public class ChallengeResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "challenge_id", nullable = false, unique = true, length = 36)
    private String challengeId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "username_snapshot", nullable = false, length = 100)
    private String usernameSnapshot;

    @Column(name = "dictionary_id", nullable = false, length = 50)
    private String dictionaryId;

    @Column(name = "total_score", nullable = false)
    private int totalScore;

    @Column(name = "puzzles_completed", nullable = false)
    private int puzzlesCompleted;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    public ChallengeResultEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(String challengeId) {
        this.challengeId = challengeId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsernameSnapshot() {
        return usernameSnapshot;
    }

    public void setUsernameSnapshot(String usernameSnapshot) {
        this.usernameSnapshot = usernameSnapshot;
    }

    public String getDictionaryId() {
        return dictionaryId;
    }

    public void setDictionaryId(String dictionaryId) {
        this.dictionaryId = dictionaryId;
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

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
