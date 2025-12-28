package com.fistraltech.server.dto;

import java.util.List;

/**
 * Response DTO containing summary statistics and per-game detail for an analysis run.
 *
 * <p><strong>Endpoint</strong>: {@code POST /api/wordai/analysis}
 *
 * <p>The response can be large because it may contain per-game guess histories.
 *
 * @author Fistral Technologies
 */
public class AnalysisResponse {
    private String algorithm;
    private String dictionaryId;
    private int totalGames;
    private int gamesWon;
    private int gamesLost;
    private double winRate;
    private Integer minAttempts;
    private Integer maxAttempts;
    private Double avgAttempts;
    private List<AnalysisGameResult> gameResults;
    private boolean completed;
    private String message;
    
    public AnalysisResponse() {}
    
    public String getAlgorithm() {
        return algorithm;
    }
    
    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }
    
    public String getDictionaryId() {
        return dictionaryId;
    }
    
    public void setDictionaryId(String dictionaryId) {
        this.dictionaryId = dictionaryId;
    }
    
    public int getTotalGames() {
        return totalGames;
    }
    
    public void setTotalGames(int totalGames) {
        this.totalGames = totalGames;
    }
    
    public int getGamesWon() {
        return gamesWon;
    }
    
    public void setGamesWon(int gamesWon) {
        this.gamesWon = gamesWon;
    }
    
    public int getGamesLost() {
        return gamesLost;
    }
    
    public void setGamesLost(int gamesLost) {
        this.gamesLost = gamesLost;
    }
    
    public double getWinRate() {
        return winRate;
    }
    
    public void setWinRate(double winRate) {
        this.winRate = winRate;
    }
    
    public Integer getMinAttempts() {
        return minAttempts;
    }
    
    public void setMinAttempts(Integer minAttempts) {
        this.minAttempts = minAttempts;
    }
    
    public Integer getMaxAttempts() {
        return maxAttempts;
    }
    
    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }
    
    public Double getAvgAttempts() {
        return avgAttempts;
    }
    
    public void setAvgAttempts(Double avgAttempts) {
        this.avgAttempts = avgAttempts;
    }
    
    public List<AnalysisGameResult> getGameResults() {
        return gameResults;
    }
    
    public void setGameResults(List<AnalysisGameResult> gameResults) {
        this.gameResults = gameResults;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
