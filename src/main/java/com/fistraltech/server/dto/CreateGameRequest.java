package com.fistraltech.server.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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
 *   "targetWord": "arose",
 *   "browserSessionId": "4f6c2fd0-90f1-48f7-95d6-4cfd44883ddd",
 *   "resumeExisting": false
 * }
 * }</pre>
 *
 * <p>All fields are optional. If {@code targetWord} is omitted, a random target is chosen.
 * If {@code dictionaryId} is provided, it takes precedence over {@code wordLength}.
 *
 * @author Fistral Technologies
 */
public class CreateGameRequest {
    @Size(min = 2, max = 20, message = "targetWord must be between 2 and 20 characters")
    @Pattern(regexp = "^[A-Za-z]+$", message = "targetWord must contain only letters")
    private String targetWord; // Optional - if not provided, a random word will be selected
    @Min(value = 2, message = "wordLength must be at least 2")
    @Max(value = 20, message = "wordLength must be at most 20")
    private Integer wordLength; // Optional - defaults to 5
    @Pattern(regexp = ".*\\S.*", message = "dictionaryId must not be blank")
    private String dictionaryId; // Optional - ID of the dictionary to use (e.g., "default", "easy", "hard")
    @Pattern(regexp = ".*\\S.*", message = "browserSessionId must not be blank")
    private String browserSessionId; // Optional - per-browser-window key used to isolate resumable sessions
    private Boolean resumeExisting; // Optional - when true, reuses an existing active session for this browser context
    
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

    public CreateGameRequest(String targetWord, Integer wordLength, String dictionaryId,
            String browserSessionId) {
        this.targetWord = targetWord;
        this.wordLength = wordLength;
        this.dictionaryId = dictionaryId;
        this.browserSessionId = browserSessionId;
    }

    public CreateGameRequest(String targetWord, Integer wordLength, String dictionaryId,
            String browserSessionId, Boolean resumeExisting) {
        this.targetWord = targetWord;
        this.wordLength = wordLength;
        this.dictionaryId = dictionaryId;
        this.browserSessionId = browserSessionId;
        this.resumeExisting = resumeExisting;
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

    public String getBrowserSessionId() {
        return browserSessionId;
    }

    public void setBrowserSessionId(String browserSessionId) {
        this.browserSessionId = browserSessionId;
    }

    public Boolean getResumeExisting() {
        return resumeExisting;
    }

    public void setResumeExisting(Boolean resumeExisting) {
        this.resumeExisting = resumeExisting;
    }
}