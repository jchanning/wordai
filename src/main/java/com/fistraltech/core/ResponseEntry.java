package com.fistraltech.core;

/**
 * Represents a single letter-status pair in a word guess response.
 * 
 * <p>This class encapsulates the feedback for one letter position in a guessed word,
 * indicating both the letter that was guessed and the result of that guess according
 * to the game's evaluation rules.
 * 
 * <p><strong>Status Encoding:</strong> The status uses a color-based system similar to Wordle:
 * <ul>
 *   <li><strong>'G' (Green):</strong> Correct letter in the correct position</li>
 *   <li><strong>'A' (Amber):</strong> Correct letter but in the wrong position</li>
 *   <li><strong>'R' (Red):</strong> Letter does not appear in the target word</li>
 *   <li><strong>'X' (eXcess):</strong> Letter appears in target, but this occurrence is excess
 *       (guessed more times than it appears in the target word)</li>
 * </ul>
 * 
 * <p><strong>Example:</strong><br>
 * For target word "AROSE" and guess "RAISE":
 * <pre>
 * Position 0: letter='R', status='A' (R is in word, wrong position)
 * Position 1: letter='A', status='A' (A is in word, wrong position)
 * Position 2: letter='I', status='R' (I is not in word)
 * Position 3: letter='S', status='G' (S is correct position)
 * Position 4: letter='E', status='G' (E is correct position)
 * </pre>
 * 
 * <p><strong>Handling Duplicate Letters:</strong><br>
 * When a letter appears multiple times in a guess, the status encoding ensures only
 * the correct number of occurrences are marked as 'G' or 'A'. Additional occurrences
 * are marked as 'X' to indicate excess.
 * 
 * <p><strong>Thread Safety:</strong> This class has no thread safety guarantees as its
 * fields are public and mutable.
 * 
 * @author Fistral Technologies
 * @see Response
 * @see WordGame
 */
public class ResponseEntry {
    
    /** The letter that was guessed at this position */
    public Character letter;
    
    /** 
     * The status of this guess, encoded as 'G', 'A', 'R', or 'X'.
     * See class documentation for detailed status meanings.
     */
    public Character status;
}
