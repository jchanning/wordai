package com.fistraltech.server.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for creating a Challenge Mode session.
 *
 * <p><strong>Example</strong>
 * <pre>{@code
 * {
 *   "dictionaryId": "default",
 *   "wordLength": 5,
 *   "browserSessionId": "4f6c2fd0-90f1-48f7-95d6-4cfd44883ddd"
 * }
 * }</pre>
 */
public class CreateChallengeRequest {
    @Pattern(regexp = ".*\\S.*", message = "dictionaryId must not be blank")
    private String dictionaryId;
    @Min(value = 2, message = "wordLength must be at least 2")
    @Max(value = 20, message = "wordLength must be at most 20")
    private Integer wordLength;
    @Pattern(regexp = ".*\\S.*", message = "browserSessionId must not be blank")
    private String browserSessionId;

    public CreateChallengeRequest() {
    }

    public String getDictionaryId() {
        return dictionaryId;
    }

    public void setDictionaryId(String dictionaryId) {
        this.dictionaryId = dictionaryId;
    }

    public Integer getWordLength() {
        return wordLength;
    }

    public void setWordLength(Integer wordLength) {
        this.wordLength = wordLength;
    }

    public String getBrowserSessionId() {
        return browserSessionId;
    }

    public void setBrowserSessionId(String browserSessionId) {
        this.browserSessionId = browserSessionId;
    }
}
