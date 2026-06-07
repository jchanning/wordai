package com.fistraltech.server.dto;

/**
 * Request DTO for creating a manual Wordle assistant session.
 *
 * <p><strong>Endpoint</strong>: {@code POST /api/v1/wordai/assistant/sessions}
 *
 * <p><strong>Example</strong>
 * <pre>{@code
 * {
 *   "dictionaryId": "default",
 *   "strategy": "ENTROPY"
 * }
 * }</pre>
 */
public class ManualAssistantCreateRequest {
    private String dictionaryId;
    private String strategy;

    public ManualAssistantCreateRequest() {
    }

    public String getDictionaryId() {
        return dictionaryId;
    }

    public void setDictionaryId(String dictionaryId) {
        this.dictionaryId = dictionaryId;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }
}