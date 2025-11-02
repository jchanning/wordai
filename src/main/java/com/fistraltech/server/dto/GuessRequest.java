package com.fistraltech.server.dto;

/**
 * Data Transfer Object for guess requests
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