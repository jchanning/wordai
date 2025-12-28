package com.fistraltech.server.dto;

/**
 * Request DTO for creating a new game session.
 *
 * <p><strong>Endpoint</strong>: {@code POST /api/wordai/games}
 *
 * <p><strong>Example</strong>
 * <pre>{@code
 * {
 *   "dictionaryId": "5",
 *   "wordLength": 5,
 *   "targetWord": "arose"
 * }
 * }</pre>
 *
 * <p>All fields are optional. If {@code targetWord} is omitted, a random target is chosen.
 * If {@code dictionaryId} is provided, it takes precedence over {@code wordLength}.
 *
 * @author Fistral Technologies
 */
public class CreateGameRequest {
    private String targetWord; // Optional - if not provided, a random word will be selected
    private Integer wordLength; // Optional - defaults to 5
    private String dictionaryId; // Optional - ID of the dictionary to use (e.g., "default", "easy", "hard")
    
    public CreateGameRequest() {}
    
    public CreateGameRequest(String targetWord, Integer wordLength) {
        this.targetWord = targetWord;
        this.wordLength = wordLength;
    }
    
    public CreateGameRequest(String targetWord, Integer wordLength, String dictionaryId) {
        this.targetWord = targetWord;
        this.wordLength = wordLength;
        this.dictionaryId = dictionaryId;
    }
    
    public String getTargetWord() {
        return targetWord;
    }
    
    public void setTargetWord(String targetWord) {
        this.targetWord = targetWord;
    }
    
    public Integer getWordLength() {
        return wordLength;
    }
    
    public void setWordLength(Integer wordLength) {
        this.wordLength = wordLength;
    }
    
    public String getDictionaryId() {
        return dictionaryId;
    }
    
    public void setDictionaryId(String dictionaryId) {
        this.dictionaryId = dictionaryId;
    }
}