package com.fistraltech.server.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for server-side dictionary analysis.
 *
 * <p><strong>Endpoint</strong>: {@code POST /api/wordai/analysis}
 *
 * <p><strong>Example</strong>
 * <pre>{@code
 * {
 *   "algorithm": "ENTROPY",
 *   "dictionaryId": "5",
 *   "maxGames": 100
 * }
 * }</pre>
 *
 * @author Fistral Technologies
 */
public class AnalysisRequest {
    @NotBlank(message = "Algorithm is required")
    private String algorithm;
    @NotBlank(message = "Dictionary ID is required")
    private String dictionaryId;
    @Min(value = 1, message = "maxGames must be at least 1")
    private Integer maxGames; // Optional: limit number of games to analyze
    
    public AnalysisRequest() {}
    
    public AnalysisRequest(String algorithm, String dictionaryId, Integer maxGames) {
        this.algorithm = algorithm;
        this.dictionaryId = dictionaryId;
        this.maxGames = maxGames;
    }
    
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
    
    public Integer getMaxGames() {
        return maxGames;
    }
    
    public void setMaxGames(Integer maxGames) {
        this.maxGames = maxGames;
    }
}
