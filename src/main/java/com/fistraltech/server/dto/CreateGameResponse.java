package com.fistraltech.server.dto;

import java.util.List;

/**
 * Response DTO returned when a new game session is created.
 *
 * <p><strong>Endpoint</strong>: {@code POST /api/wordai/games}
 *
 * <p><strong>Example</strong>
 * <pre>{@code
 * {
 *   "gameId": "...",
 *   "wordLength": 5,
 *   "maxAttempts": 6,
 *   "message": "Game created successfully! Start guessing!",
 *   "dictionaryMetrics": {
 *     "totalWords": 2315,
 *     "letterCount": 11575,
 *     "uniqueCharacters": 26,
 *     "columnLengths": [26,26,26,26,26]
 *   }
 * }
 * }</pre>
 *
 * @author Fistral Technologies
 */
public class CreateGameResponse {
    private String gameId;
    private int wordLength;
    private int maxAttempts;
    private String message;
    private DictionaryMetrics dictionaryMetrics;
    
    public CreateGameResponse() {}
    
    public CreateGameResponse(String gameId, int wordLength, int maxAttempts) {
        this.gameId = gameId;
        this.wordLength = wordLength;
        this.maxAttempts = maxAttempts;
        this.message = "Game created successfully! Start guessing!";
    }
    
    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }
    
    public int getWordLength() { return wordLength; }
    public void setWordLength(int wordLength) { this.wordLength = wordLength; }
    
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public DictionaryMetrics getDictionaryMetrics() { return dictionaryMetrics; }
    public void setDictionaryMetrics(DictionaryMetrics dictionaryMetrics) { this.dictionaryMetrics = dictionaryMetrics; }
    
    /**
     * Metrics describing the dictionary state at game creation time.
     */
    public static class DictionaryMetrics {
        private int totalWords;
        private int letterCount;
        private int uniqueCharacters;
        private List<Integer> columnLengths;
        private java.util.Map<Character, List<Integer>> occurrenceCountByPosition;
        private List<Character> mostFrequentCharByPosition;
        
        public DictionaryMetrics() {}
        
        public DictionaryMetrics(int totalWords, int letterCount, int uniqueCharacters, List<Integer> columnLengths) {
            this.totalWords = totalWords;
            this.letterCount = letterCount;
            this.uniqueCharacters = uniqueCharacters;
            this.columnLengths = columnLengths;
        }
        
        public int getTotalWords() { return totalWords; }
        public void setTotalWords(int totalWords) { this.totalWords = totalWords; }
        
        public int getLetterCount() { return letterCount; }
        public void setLetterCount(int letterCount) { this.letterCount = letterCount; }
        
        public int getUniqueCharacters() { return uniqueCharacters; }
        public void setUniqueCharacters(int uniqueCharacters) { this.uniqueCharacters = uniqueCharacters; }
        
        public List<Integer> getColumnLengths() { return columnLengths; }
        public void setColumnLengths(List<Integer> columnLengths) { this.columnLengths = columnLengths; }
        
        public java.util.Map<Character, List<Integer>> getOccurrenceCountByPosition() { return occurrenceCountByPosition; }
        public void setOccurrenceCountByPosition(java.util.Map<Character, List<Integer>> occurrenceCountByPosition) { 
            this.occurrenceCountByPosition = occurrenceCountByPosition; 
        }
        
        public List<Character> getMostFrequentCharByPosition() { return mostFrequentCharByPosition; }
        public void setMostFrequentCharByPosition(List<Character> mostFrequentCharByPosition) { 
            this.mostFrequentCharByPosition = mostFrequentCharByPosition; 
        }
    }
}