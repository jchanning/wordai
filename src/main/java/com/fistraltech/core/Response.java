package com.fistraltech.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the result of a single guess in the WordAI game.
 * 
 * <p>A Response object encapsulates all information about how a guessed word
 * compares to the target word, including:
 * <ul>
 *   <li>The guessed word itself</li>
 *   <li>Per-letter status indicators (correct position, wrong position, or absent)</li>
 *   <li>Whether the guess was correct (winner)</li>
 *   <li>The number of remaining valid words after applying this guess's constraints</li>
 * </ul>
 * 
 * <p><strong>Status Codes:</strong>
 * Each letter in the guessed word receives a status character:
 * <ul>
 *   <li><strong>'G'</strong> (Green) - Letter is correct and in the correct position</li>
 *   <li><strong>'A'</strong> (Amber/Yellow) - Letter exists in the target word but is in the wrong position</li>
 *   <li><strong>'R'</strong> (Red/Gray) - Letter does not exist in the target word</li>
 *   <li><strong>'X'</strong> (Excess) - Letter exists more times in the guessed word than in the target word</li>
 * </ul>
 * 
 * <p><strong>Example:</strong>
 * <pre>{@code
 * // Target word: AROSE, Guess: RAISE
 * Response response = new Response("RAISE");
 * response.setStatus('R', 'A'); // R exists but wrong position
 * response.setStatus('A', 'A'); // A exists but wrong position
 * response.setStatus('I', 'R'); // I not in word
 * response.setStatus('S', 'G'); // S correct position
 * response.setStatus('E', 'G'); // E correct position
 * response.setWinner(false);
 * response.setRemainingWordsCount(42);
 * }</pre>
 * 
 * @author Fistral Technologies
 * @version 1.0
 * @see ResponseEntry
 * @see WordGame
 */
public class Response {
    
    /** The word that was guessed by the player */
    private final String word;
    
    /** 
     * List of ResponseEntry objects, one for each letter in the guessed word.
     * Each entry contains the letter and its status code (G/A/R).
     */
    private final List<ResponseEntry> status = new ArrayList<>();
    
    /** 
     * Indicates whether this guess was correct (matched the target word exactly).
     * True if all letters are in correct positions, false otherwise.
     */
    private boolean isWinner = false;
    
    /** 
     * The number of valid words remaining after applying this guess's constraints.
     * A value of -1 indicates this information has not been calculated or set.
     * This is useful for analytics and bot strategy development.
     */
    private int remainingWordsCount = -1;

    /**
     * Constructs a new Response object for the specified guessed word.
     * 
     * <p>After construction, use {@link #setStatus(char, char)} to add status
     * information for each letter, and {@link #setWinner(boolean)} to indicate
     * if the guess was correct.
     * 
     * @param word the word that was guessed (typically uppercase, must not be null)
     * @see #setStatus(char, char)
     * @see #setWinner(boolean)
     */
    public Response(String word){
        this.word = word;
    }

    /**
     * Adds a status entry for a single letter in the guessed word.
     * 
     * <p>This method should be called once for each letter in the word, in order
     * from left to right. The status character indicates how that letter compares
     * to the target word:
     * <ul>
     *   <li><strong>'G'</strong> - Correct letter in correct position (Green)</li>
     *   <li><strong>'A'</strong> - Correct letter in wrong position (Amber/Yellow)</li>
     *   <li><strong>'R'</strong> - Letter not in target word (Red/Gray)</li>
     * </ul>
     * 
     * <p><strong>Example:</strong>
     * <pre>{@code
     * response.setStatus('H', 'R'); // H is not in the target word
     * response.setStatus('O', 'A'); // O is in the word but wrong position
     * response.setStatus('U', 'G'); // U is correct and in right position
     * }</pre>
     * 
     * @param k the letter from the guessed word
     * @param v the status code: 'G' (Green/Correct), 'A' (Amber/Present), or 'R' (Red/Absent)
     * @see ResponseEntry
     * @see #getStatuses()
     */
    public void setStatus( char k, char v){
        ResponseEntry e = new ResponseEntry();
        e.letter = k;
        e.status = v;
        status.add(e);
    }

    /**
     * Returns the list of status entries for each letter in the guessed word.
     * 
     * <p>Each {@link ResponseEntry} in the list contains a letter and its corresponding
     * status code. The list is in the same order as the letters in the guessed word.
     * 
     * @return an unmodifiable view of the status entries (letter + status code pairs)
     * @see ResponseEntry
     * @see #setStatus(char, char)
     */
    public List<ResponseEntry> getStatuses() {
        return status;
    }

    /**
     * Sets whether this guess resulted in winning the game.
     * 
     * <p>A guess is considered a winner if all letters match the target word
     * in the correct positions (all status codes are 'G').
     * 
     * @param winner {@code true} if the guess matched the target word exactly, {@code false} otherwise
     * @see #getWinner()
     */
    public void setWinner(boolean winner){
        this.isWinner = winner;
    }

    /**
     * Returns whether this guess resulted in winning the game.
     * 
     * <p>Returns {@code true} only if the guessed word exactly matches the target word.
     * 
     * @return {@code true} if this was a winning guess, {@code false} otherwise
     * @see #setWinner(boolean)
     */
    public boolean getWinner(){
        return this.isWinner;
    }

    /**
     * Returns the word that was guessed.
     * 
     * @return the guessed word as a String (typically uppercase)
     */
    public String getWord() {
        return word;
    }

    /**
     * Returns the number of valid words remaining after this guess.
     * 
     * <p>This value represents how many words in the dictionary are still consistent
     * with all the constraints learned from this and previous guesses. It's useful for:
     * <ul>
     *   <li>Analytics - tracking how quickly the solution space narrows</li>
     *   <li>Bot strategy - choosing guesses that maximize information gain</li>
     *   <li>UI feedback - showing players their progress</li>
     * </ul>
     * 
     * @return the count of remaining valid words, or -1 if not calculated/set
     * @see #setRemainingWordsCount(int)
     */
    public int getRemainingWordsCount() {
        return remainingWordsCount;
    }

    /**
     * Sets the number of valid words remaining after this guess.
     * 
     * <p>This is typically calculated by filtering the dictionary to only include
     * words that satisfy all constraints from this and previous guesses.
     * 
     * @param remainingWordsCount the number of valid words remaining, or -1 if unknown
     * @see #getRemainingWordsCount()
     */
    public void setRemainingWordsCount(int remainingWordsCount) {
        this.remainingWordsCount = remainingWordsCount;
    }

    /**
     * Returns a compact string representation of the response status codes.
     * 
     * <p>Produces a string containing only the status characters (G/A/R) for each
     * letter, in order. This is useful for compact logging or pattern matching.
     * 
     * <p><strong>Example:</strong> For the word "RAISE" with status [A, A, R, G, G],
     * this returns "AARGG".
     * 
     * @return a string of status codes (e.g., "GARGG")
     * @see #resultToString()
     */
    @Override
    public String toString(){
        String result = "";
        for(ResponseEntry e: status){
              result = result + e.status;
          }
        return result;
    }

    /**
     * Returns a human-readable description of this response.
     * 
     * <p>Includes the guessed word and whether it was a winning guess.
     * This is useful for debugging and detailed logging.
     * 
     * <p><strong>Example output:</strong> "Word: RAISE is winner = false"
     * 
     * @return a formatted string describing the word and win status
     * @see #toString()
     */
    public String resultToString(){
        return "Word: " + word + " is winner = " + isWinner;
    }
}
