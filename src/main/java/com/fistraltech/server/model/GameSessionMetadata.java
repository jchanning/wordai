package com.fistraltech.server.model;

/**
 * Mutable session metadata that is orthogonal to the active game and suggestion context.
 */
final class GameSessionMetadata {

    private boolean gameEnded;
    private String dictionaryId = "default";
    private Long userId;
    private String browserSessionId;

    boolean isGameEnded() {
        return gameEnded;
    }

    void setGameEnded(boolean gameEnded) {
        this.gameEnded = gameEnded;
    }

    String getDictionaryId() {
        return dictionaryId;
    }

    void setDictionaryId(String dictionaryId) {
        this.dictionaryId = dictionaryId;
    }

    Long getUserId() {
        return userId;
    }

    void setUserId(Long userId) {
        this.userId = userId;
    }

    String getBrowserSessionId() {
        return browserSessionId;
    }

    void setBrowserSessionId(String browserSessionId) {
        this.browserSessionId = browserSessionId;
    }
}