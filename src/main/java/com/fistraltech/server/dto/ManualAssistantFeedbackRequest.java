package com.fistraltech.server.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for submitting manual Wordle feedback.
 *
 * <p><strong>Endpoint</strong>: {@code POST /api/v1/wordai/assistant/sessions/{sessionId}/feedback}
 *
 * <p><strong>Example</strong>
 * <pre>{@code
 * {
 *   "guessedWord": "crane",
 *   "feedbackPattern": "🟨⬛⬛🟩⬛"
 * }
 * }</pre>
 */
public class ManualAssistantFeedbackRequest {
    @NotBlank(message = "Guessed word is required")
    private String guessedWord;

    @NotBlank(message = "Feedback pattern is required")
    private String feedbackPattern;

    public ManualAssistantFeedbackRequest() {
    }

    public String getGuessedWord() {
        return guessedWord;
    }

    public void setGuessedWord(String guessedWord) {
        this.guessedWord = guessedWord;
    }

    public String getFeedbackPattern() {
        return feedbackPattern;
    }

    public void setFeedbackPattern(String feedbackPattern) {
        this.feedbackPattern = feedbackPattern;
    }
}