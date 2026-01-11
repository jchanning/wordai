package com.fistraltech.server.dto;

import java.util.List;

/**
 * DTO representing the result of a single game within an analysis run.
 *
 * <p>Each result includes the target word, win/loss state, attempt count, and (optionally)
 * the full sequence of guesses.
 *
 * @author Fistral Technologies
 */
public class AnalysisGameResult {
    private String targetWord;
    private int attempts;
    private boolean won;
    private String algorithm;
    private List<GuessDetails> guesses;
    
    public AnalysisGameResult() {}
    
    public AnalysisGameResult(String targetWord, int attempts, boolean won, String algorithm, List<GuessDetails> guesses) {
        this.targetWord = targetWord;
        this.attempts = attempts;
        this.won = won;
        this.algorithm = algorithm;
        this.guesses = guesses;
    }
    
    public String getTargetWord() {
        return targetWord;
    }
    
    public void setTargetWord(String targetWord) {
        this.targetWord = targetWord;
    }
    
    public int getAttempts() {
        return attempts;
    }
    
    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }
    
    public boolean isWon() {
        return won;
    }
    
    public void setWon(boolean won) {
        this.won = won;
    }
    
    public String getAlgorithm() {
        return algorithm;
    }
    
    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }
    
    public List<GuessDetails> getGuesses() {
        return guesses;
    }
    
    public void setGuesses(List<GuessDetails> guesses) {
        this.guesses = guesses;
    }
    
    /**
     * DTO representing a single guess within a game during analysis.
     */
    public static class GuessDetails {
        private int attemptNumber;
        private String guess;
        private String response;
        private int remainingWords;
        private int letterCount;

        public GuessDetails() {}

        public GuessDetails(int attemptNumber, String guess, String response, int remainingWords, int letterCount) {
            this.attemptNumber = attemptNumber;
            this.guess = guess;
            this.response = response;
            this.remainingWords = remainingWords;
            this.letterCount = letterCount;
        }
        
        public int getAttemptNumber() {
            return attemptNumber;
        }
        
        public void setAttemptNumber(int attemptNumber) {
            this.attemptNumber = attemptNumber;
        }
        
        public String getGuess() {
            return guess;
        }
        
        public void setGuess(String guess) {
            this.guess = guess;
        }
        
        public String getResponse() {
            return response;
        }
        
        public void setResponse(String response) {
            this.response = response;
        }
        
        public int getRemainingWords() {
            return remainingWords;
        }
        
        public void setRemainingWords(int remainingWords) {
            this.remainingWords = remainingWords;
        }
        
        public int getLetterCount() {
            return letterCount;
        }
        
        public void setLetterCount(int letterCount) {
            this.letterCount = letterCount;
        }
    }
}
