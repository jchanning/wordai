package com.fistraltech.server.model;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * In-memory representation of one active Challenge Mode run.
 *
 * <p>A challenge is a sequence of 10 single-word puzzles. Each puzzle is backed by a normal
 * {@link GameSession}; challenge state adds timing, score, pause/skip usage, and cumulative
 * progress across the sequence.
 */
public class ChallengeSession {
    public static final int TOTAL_PUZZLES = 10;
    public static final int MAX_ATTEMPTS_PER_PUZZLE = 6;
    public static final int START_TIME_LIMIT_SECONDS = 120;
    public static final int TIME_DECREMENT_SECONDS = 10;
    public static final int MIN_TIME_LIMIT_SECONDS = 10;
    public static final int START_ASSISTS = 3;
    public static final int MIN_ASSISTS = 1;
    public static final int ASSIST_DECREMENT_INTERVAL = 3;
    public static final int SKIP_PENALTY_POINTS = 20;
    public static final int PAUSE_EXTENSION_SECONDS = 10;

    private final String challengeId;
    private final String dictionaryId;
    private final Long userId;
    private final String browserSessionId;
    private final List<String> targetWords;
    private final Instant createdAt;
    private final List<PuzzleSummary> completedPuzzles = new ArrayList<>();

    private GameSession currentPuzzleSession;
    private int currentPuzzleIndex;
    private int totalScore;
    private int currentPuzzleAssistsRemaining;
    private boolean pauseUsed;
    private boolean skipUsed;
    private boolean resultPersisted;
    private String status;
    private Instant currentPuzzleStartedAt;
    private Instant currentPuzzleDeadline;
    private Instant updatedAt;

    public ChallengeSession(String challengeId, String dictionaryId, Long userId,
            String browserSessionId, List<String> targetWords, Instant createdAt) {
        this.challengeId = challengeId;
        this.dictionaryId = dictionaryId;
        this.userId = userId;
        this.browserSessionId = browserSessionId;
        this.targetWords = new ArrayList<>(targetWords);
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.status = "ACTIVE";
    }

    public String getChallengeId() {
        return challengeId;
    }

    public String getDictionaryId() {
        return dictionaryId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getBrowserSessionId() {
        return browserSessionId;
    }

    public List<String> getTargetWords() {
        return Collections.unmodifiableList(targetWords);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<PuzzleSummary> getCompletedPuzzles() {
        return Collections.unmodifiableList(completedPuzzles);
    }

    public void addCompletedPuzzle(PuzzleSummary summary) {
        completedPuzzles.add(summary);
    }

    public GameSession getCurrentPuzzleSession() {
        return currentPuzzleSession;
    }

    public void setCurrentPuzzleSession(GameSession currentPuzzleSession) {
        this.currentPuzzleSession = currentPuzzleSession;
    }

    public int getCurrentPuzzleIndex() {
        return currentPuzzleIndex;
    }

    public void setCurrentPuzzleIndex(int currentPuzzleIndex) {
        this.currentPuzzleIndex = currentPuzzleIndex;
    }

    public int getCurrentPuzzleNumber() {
        return Math.min(currentPuzzleIndex + 1, TOTAL_PUZZLES);
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public int getCurrentPuzzleAssistsRemaining() {
        return currentPuzzleAssistsRemaining;
    }

    public void setCurrentPuzzleAssistsRemaining(int currentPuzzleAssistsRemaining) {
        this.currentPuzzleAssistsRemaining = currentPuzzleAssistsRemaining;
    }

    public boolean isPauseUsed() {
        return pauseUsed;
    }

    public void setPauseUsed(boolean pauseUsed) {
        this.pauseUsed = pauseUsed;
    }

    public boolean isSkipUsed() {
        return skipUsed;
    }

    public void setSkipUsed(boolean skipUsed) {
        this.skipUsed = skipUsed;
    }

    public boolean isResultPersisted() {
        return resultPersisted;
    }

    public void setResultPersisted(boolean resultPersisted) {
        this.resultPersisted = resultPersisted;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCurrentPuzzleStartedAt() {
        return currentPuzzleStartedAt;
    }

    public void setCurrentPuzzleStartedAt(Instant currentPuzzleStartedAt) {
        this.currentPuzzleStartedAt = currentPuzzleStartedAt;
    }

    public Instant getCurrentPuzzleDeadline() {
        return currentPuzzleDeadline;
    }

    public void setCurrentPuzzleDeadline(Instant currentPuzzleDeadline) {
        this.currentPuzzleDeadline = currentPuzzleDeadline;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getPuzzlesCompleted() {
        return completedPuzzles.size();
    }

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    public boolean isChallengeComplete() {
        return "COMPLETED".equals(status);
    }

    public boolean isChallengeFailed() {
        return status.startsWith("FAILED");
    }

    public int getCurrentPuzzleTimeLimitSeconds() {
        return computeTimeLimitSeconds(currentPuzzleIndex);
    }

    public int getCurrentPuzzleAssistAllowance() {
        return computeAssistAllowance(currentPuzzleIndex);
    }

    public long getSecondsRemaining(Instant now) {
        if (currentPuzzleDeadline == null) {
            return 0;
        }
        return Math.max(0, Duration.between(now, currentPuzzleDeadline).getSeconds());
    }

    public static int computeTimeLimitSeconds(int puzzleIndex) {
        return Math.max(MIN_TIME_LIMIT_SECONDS,
                START_TIME_LIMIT_SECONDS - (puzzleIndex * TIME_DECREMENT_SECONDS));
    }

    public static int computeAssistAllowance(int puzzleIndex) {
        return Math.max(MIN_ASSISTS,
                START_ASSISTS - (puzzleIndex / ASSIST_DECREMENT_INTERVAL));
    }

    /**
     * Summary of one finished puzzle within a challenge.
     */
    public static class PuzzleSummary {
        private int puzzleNumber;
        private String targetWord;
        private String status;
        private int scoreAwarded;
        private int attemptsUsed;
        private int maxAttempts;
        private long timeTakenSeconds;
        private int timeLimitSeconds;

        public PuzzleSummary() {
        }

        public PuzzleSummary(int puzzleNumber, String targetWord, String status, int scoreAwarded,
                int attemptsUsed, int maxAttempts, long timeTakenSeconds, int timeLimitSeconds) {
            this.puzzleNumber = puzzleNumber;
            this.targetWord = targetWord;
            this.status = status;
            this.scoreAwarded = scoreAwarded;
            this.attemptsUsed = attemptsUsed;
            this.maxAttempts = maxAttempts;
            this.timeTakenSeconds = timeTakenSeconds;
            this.timeLimitSeconds = timeLimitSeconds;
        }

        public int getPuzzleNumber() {
            return puzzleNumber;
        }

        public void setPuzzleNumber(int puzzleNumber) {
            this.puzzleNumber = puzzleNumber;
        }

        public String getTargetWord() {
            return targetWord;
        }

        public void setTargetWord(String targetWord) {
            this.targetWord = targetWord;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public int getScoreAwarded() {
            return scoreAwarded;
        }

        public void setScoreAwarded(int scoreAwarded) {
            this.scoreAwarded = scoreAwarded;
        }

        public int getAttemptsUsed() {
            return attemptsUsed;
        }

        public void setAttemptsUsed(int attemptsUsed) {
            this.attemptsUsed = attemptsUsed;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getTimeTakenSeconds() {
            return timeTakenSeconds;
        }

        public void setTimeTakenSeconds(long timeTakenSeconds) {
            this.timeTakenSeconds = timeTakenSeconds;
        }

        public int getTimeLimitSeconds() {
            return timeLimitSeconds;
        }

        public void setTimeLimitSeconds(int timeLimitSeconds) {
            this.timeLimitSeconds = timeLimitSeconds;
        }
    }
}
