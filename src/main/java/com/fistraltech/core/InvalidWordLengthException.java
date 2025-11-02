package com.fistraltech.core;

/**
 * Exception thrown when a word's length doesn't match the expected word length.
 * 
 * <p>This specialized exception extends {@link InvalidWordException} to specifically
 * handle cases where a word fails validation due to incorrect length. This distinction
 * allows for more precise error handling and user feedback.
 * 
 * <p><strong>Common Scenarios:</strong>
 * <ul>
 *   <li>Player attempts to guess a 4-letter word in a 5-letter game</li>
 *   <li>Attempting to add words to a dictionary when they don't match the dictionary's word length</li>
 *   <li>Programmatic errors where word lengths are mismatched</li>
 * </ul>
 * 
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * public void addWord(String word) throws InvalidWordLengthException {
 *     if (word.length() != expectedLength) {
 *         throw new InvalidWordLengthException(
 *             String.format("Expected %d letters, got %d", expectedLength, word.length())
 *         );
 *     }
 *     // ... add the word
 * }
 * }</pre>
 * 
 * <p><strong>Error Messages:</strong> It's recommended to include both the expected
 * and actual word lengths in the error message for clarity.
 * 
 * @author Fistral Technologies
 * @see InvalidWordException
 * @see Dictionary
 * @see WordGame
 */
public class InvalidWordLengthException extends InvalidWordException {
    
    /**
     * Constructs a new InvalidWordLengthException with the specified detail message.
     * 
     * @param message the detail message explaining the length mismatch,
     *                ideally including both expected and actual lengths
     */
    public InvalidWordLengthException(String message) {
        super(message);
    }
}
