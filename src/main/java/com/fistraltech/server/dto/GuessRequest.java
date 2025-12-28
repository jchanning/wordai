package com.fistraltech.server.dto;

/**
 * Request DTO for submitting a guess.
 *
 * <p><strong>Endpoint</strong>: {@code POST /api/wordai/games/{gameId}/guess}
 *
 * <p><strong>Example</strong>
 * <pre>{@code
 * { "word": "CRANE" }
 * }</pre>
 *
 * @author Fistral Technologies
 */
public class GuessRequest {
    private String word;
    
    public GuessRequest() {}
    
    public GuessRequest(String word) {
        this.word = word;
    }
    
    public String getWord() {
        return word;
    }
    
    public void setWord(String word) {
        this.word = word;
    }
}