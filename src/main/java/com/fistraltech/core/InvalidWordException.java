package com.fistraltech.core;

/**
 * Exception thrown when an invalid word is encountered during game play.
 * 
 * <p>This exception is used throughout the WordAI system to signal various word-related errors:
 * <ul>
 *   <li>Word not found in the dictionary</li>
 *   <li>Word length doesn't match the expected length</li>
 *   <li>Dictionary not initialized when attempting to set a target word</li>
 *   <li>Other word validation failures</li>
 * </ul>
 * 
 * <p><strong>Common Scenarios:</strong>
 * <ul>
 *   <li><strong>Invalid Guess:</strong> Player attempts to guess a word not in the valid words dictionary</li>
 *   <li><strong>Wrong Length:</strong> Word doesn't match the game's configured word length (use {@link InvalidWordLengthException})</li>
 *   <li><strong>Uninitialized Dictionary:</strong> Attempting to set a target word before loading the dictionary</li>
 * </ul>
 * 
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * public void setTargetWord(String word) throws InvalidWordException {
 *     if (!dictionary.contains(word)) {
 *         throw new InvalidWordException("Word is not in dictionary");
 *     }
 *     // ... set the word
 * }
 * }</pre>
 * 
 * @author Fistral Technologies
 * @see InvalidWordLengthException
 * @see WordGame
 * @see Dictionary
 */
public class InvalidWordException extends Exception {
    
    /**
     * Constructs a new InvalidWordException with the specified detail message.
     * 
     * @param message the detail message explaining why the word is invalid
     */
    public InvalidWordException(String message) {
        super(message);
    }
}
