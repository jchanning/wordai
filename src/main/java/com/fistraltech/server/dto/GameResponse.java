package com.fistraltech.server.dto;

import java.util.List;
import java.util.stream.Collectors;

import com.fistraltech.core.Response;

/**
 * Response DTO returned after a guess is applied.
 *
 * <p><strong>Endpoint</strong>: {@code POST /api/wordai/games/{gameId}/guess}
 *
 * <p><strong>Example (simplified)</strong>
 * <pre>{@code
 * {
 *   "gameId": "...",
 *   "gameWon": false,
 *   "gameOver": false,
 *   "attemptNumber": 1,
 *   "maxAttempts": 6,
 *   "guessedWord": "CRANE",
 *   "results": [
 *     {"letter":"C","status":"R"},
 *     {"letter":"R","status":"A"},
 *     {"letter":"A","status":"G"},
 *     {"letter":"N","status":"R"},
 *     {"letter":"E","status":"R"}
 *   ],
 *   "remainingWordsCount": 128,
 *   "message": "Keep guessing!"
 * }
 * }</pre>
 *
 * <p><strong>Status codes</strong>
 * <ul>
 *   <li>{@code G} = correct letter in correct position (green)</li>
 *   <li>{@code A} = correct letter in wrong position (amber)</li>
 *   <li>{@code R} = letter not present (red)</li>
 * </ul>
 *
 * @author Fistral Technologies
 */
public class GameResponse {
    private String gameId;
    private boolean gameWon;
    private boolean gameOver;
    private int attemptNumber;
    private int maxAttempts;
    private String guessedWord;
    private List<LetterResult> results;
    private String message;
    private Integer remainingWordsCount;
    private String suggestedWord;
    private DictionaryMetrics dictionaryMetrics;
    
    public GameResponse() {}
    
    public GameResponse(String gameId, Response response, int attemptNumber, int maxAttempts) {
        this.gameId = gameId;
        this.gameWon = response.getWinner();
        this.gameOver = response.getWinner() || attemptNumber >= maxAttempts;
        this.attemptNumber = attemptNumber;
        this.maxAttempts = maxAttempts;
        this.guessedWord = response.getWord();
        this.results = response.getStatuses().stream()
                .map(entry -> new LetterResult(entry.letter, entry.status))
                .collect(Collectors.toList());
        this.remainingWordsCount = response.getRemainingWordsCount() >= 0 ? response.getRemainingWordsCount() : null;
        
        if (gameWon) {
            this.message = "Congratulations! You won!";
        } else if (gameOver) {
            this.message = "Game over! You've used all your attempts.";
        } else {
            this.message = "Keep guessing!";
        }
    }
    
    // Getters and setters
    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }
    
    public boolean isGameWon() { return gameWon; }
    public void setGameWon(boolean gameWon) { this.gameWon = gameWon; }
    
    public boolean isGameOver() { return gameOver; }
    public void setGameOver(boolean gameOver) { this.gameOver = gameOver; }
    
    public int getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(int attemptNumber) { this.attemptNumber = attemptNumber; }
    
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    
    public String getGuessedWord() { return guessedWord; }
    public void setGuessedWord(String guessedWord) { this.guessedWord = guessedWord; }
    
    public List<LetterResult> getResults() { return results; }
    public void setResults(List<LetterResult> results) { this.results = results; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public Integer getRemainingWordsCount() { return remainingWordsCount; }
    public void setRemainingWordsCount(Integer remainingWordsCount) { this.remainingWordsCount = remainingWordsCount; }
    
    public String getSuggestedWord() { return suggestedWord; }
    public void setSuggestedWord(String suggestedWord) { this.suggestedWord = suggestedWord; }
    
    public DictionaryMetrics getDictionaryMetrics() { return dictionaryMetrics; }
    public void setDictionaryMetrics(DictionaryMetrics dictionaryMetrics) { this.dictionaryMetrics = dictionaryMetrics; }
    
    /**
     * Metrics about the filtered dictionary state
     */
    public static class DictionaryMetrics {
        private int letterCount;
        private int uniqueCharacters;
        private List<Integer> columnLengths;
        private java.util.Map<Character, List<Integer>> occurrenceCountByPosition;
        private List<Character> mostFrequentCharByPosition;
        
        public DictionaryMetrics() {}
        
        public DictionaryMetrics(int letterCount, int uniqueCharacters, List<Integer> columnLengths) {
            this.letterCount = letterCount;
            this.uniqueCharacters = uniqueCharacters;
            this.columnLengths = columnLengths;
        }
        
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
    
    /**
     * DTO representing the result for a single letter in a guess.
     */
    public static class LetterResult {
        private char letter;
        private char status; // 'G' = correct position, 'A' = present elsewhere, 'R' = absent
        
        public LetterResult() {}
        
        public LetterResult(char letter, char status) {
            this.letter = letter;
            this.status = status;
        }
        
        public char getLetter() { return letter; }
        public void setLetter(char letter) { this.letter = letter; }
        
        public char getStatus() { return status; }
        public void setStatus(char status) { this.status = status; }
        
        public String getStatusDescription() {
            return switch (status) {
                case 'G' -> "correct";
                case 'A' -> "present";
                case 'R' -> "absent";
                default -> "unknown";
            };
        }
    }
}