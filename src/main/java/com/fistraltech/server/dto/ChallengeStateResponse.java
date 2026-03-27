package com.fistraltech.server.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO representing the current state of a Challenge Mode session.
 *
 * <p><strong>Example</strong>
 * <pre>{@code
 * {
 *   "challengeId": "...",
 *   "status": "ACTIVE",
 *   "currentPuzzleNumber": 1,
 *   "totalScore": 0,
 *   "secondsRemaining": 60,
 *   "currentPuzzleAssistsRemaining": 3,
 *   "pauseUsed": false,
 *   "skipUsed": false
 * }
 * }</pre>
 */
public class ChallengeStateResponse {
    private String challengeId;
    private String dictionaryId;
    private String status;
    private int totalScore;
    private int totalPuzzles;
    private int currentPuzzleNumber;
    private int puzzlesCompleted;
    private int currentPuzzleTimeLimitSeconds;
    private long secondsRemaining;
    private int currentPuzzleAssistsRemaining;
    private int currentAttempts;
    private int maxAttempts;
    private boolean pauseUsed;
    private boolean skipUsed;
    private boolean challengeComplete;
    private boolean challengeFailed;
    private String message;
    private String suggestedWord;
    private String revealedTargetWord;
    private LastGuessResult lastGuess;
    private List<PuzzleSummary> completedPuzzles = new ArrayList<>();

    public ChallengeStateResponse() {
    }

    public String getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(String challengeId) {
        this.challengeId = challengeId;
    }

    public String getDictionaryId() {
        return dictionaryId;
    }

    public void setDictionaryId(String dictionaryId) {
        this.dictionaryId = dictionaryId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public int getTotalPuzzles() {
        return totalPuzzles;
    }

    public void setTotalPuzzles(int totalPuzzles) {
        this.totalPuzzles = totalPuzzles;
    }

    public int getCurrentPuzzleNumber() {
        return currentPuzzleNumber;
    }

    public void setCurrentPuzzleNumber(int currentPuzzleNumber) {
        this.currentPuzzleNumber = currentPuzzleNumber;
    }

    public int getPuzzlesCompleted() {
        return puzzlesCompleted;
    }

    public void setPuzzlesCompleted(int puzzlesCompleted) {
        this.puzzlesCompleted = puzzlesCompleted;
    }

    public int getCurrentPuzzleTimeLimitSeconds() {
        return currentPuzzleTimeLimitSeconds;
    }

    public void setCurrentPuzzleTimeLimitSeconds(int currentPuzzleTimeLimitSeconds) {
        this.currentPuzzleTimeLimitSeconds = currentPuzzleTimeLimitSeconds;
    }

    public long getSecondsRemaining() {
        return secondsRemaining;
    }

    public void setSecondsRemaining(long secondsRemaining) {
        this.secondsRemaining = secondsRemaining;
    }

    public int getCurrentPuzzleAssistsRemaining() {
        return currentPuzzleAssistsRemaining;
    }

    public void setCurrentPuzzleAssistsRemaining(int currentPuzzleAssistsRemaining) {
        this.currentPuzzleAssistsRemaining = currentPuzzleAssistsRemaining;
    }

    public int getCurrentAttempts() {
        return currentAttempts;
    }

    public void setCurrentAttempts(int currentAttempts) {
        this.currentAttempts = currentAttempts;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
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

    public boolean isChallengeComplete() {
        return challengeComplete;
    }

    public void setChallengeComplete(boolean challengeComplete) {
        this.challengeComplete = challengeComplete;
    }

    public boolean isChallengeFailed() {
        return challengeFailed;
    }

    public void setChallengeFailed(boolean challengeFailed) {
        this.challengeFailed = challengeFailed;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSuggestedWord() {
        return suggestedWord;
    }

    public void setSuggestedWord(String suggestedWord) {
        this.suggestedWord = suggestedWord;
    }

    public String getRevealedTargetWord() {
        return revealedTargetWord;
    }

    public void setRevealedTargetWord(String revealedTargetWord) {
        this.revealedTargetWord = revealedTargetWord;
    }

    public LastGuessResult getLastGuess() {
        return lastGuess;
    }

    public void setLastGuess(LastGuessResult lastGuess) {
        this.lastGuess = lastGuess;
    }

    public List<PuzzleSummary> getCompletedPuzzles() {
        return completedPuzzles;
    }

    public void setCompletedPuzzles(List<PuzzleSummary> completedPuzzles) {
        this.completedPuzzles = completedPuzzles;
    }

    public static class LastGuessResult {
        private String guessedWord;
        private boolean puzzleSolved;
        private boolean puzzleOver;
        private int attemptNumber;
        private int maxAttempts;
        private Integer remainingWordsCount;
        private List<LetterResult> results = new ArrayList<>();

        public LastGuessResult() {
        }

        public String getGuessedWord() {
            return guessedWord;
        }

        public void setGuessedWord(String guessedWord) {
            this.guessedWord = guessedWord;
        }

        public boolean isPuzzleSolved() {
            return puzzleSolved;
        }

        public void setPuzzleSolved(boolean puzzleSolved) {
            this.puzzleSolved = puzzleSolved;
        }

        public boolean isPuzzleOver() {
            return puzzleOver;
        }

        public void setPuzzleOver(boolean puzzleOver) {
            this.puzzleOver = puzzleOver;
        }

        public int getAttemptNumber() {
            return attemptNumber;
        }

        public void setAttemptNumber(int attemptNumber) {
            this.attemptNumber = attemptNumber;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Integer getRemainingWordsCount() {
            return remainingWordsCount;
        }

        public void setRemainingWordsCount(Integer remainingWordsCount) {
            this.remainingWordsCount = remainingWordsCount;
        }

        public List<LetterResult> getResults() {
            return results;
        }

        public void setResults(List<LetterResult> results) {
            this.results = results;
        }
    }

    public static class LetterResult {
        private char letter;
        private char status;

        public LetterResult() {
        }

        public LetterResult(char letter, char status) {
            this.letter = letter;
            this.status = status;
        }

        public char getLetter() {
            return letter;
        }

        public void setLetter(char letter) {
            this.letter = letter;
        }

        public char getStatus() {
            return status;
        }

        public void setStatus(char status) {
            this.status = status;
        }
    }

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
