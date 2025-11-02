package com.fistraltech.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents a vertical slice (column) of a word dictionary where all words have the same length.
 * 
 * <p>This class provides an efficient data structure for analyzing letter distributions at a specific
 * position across all words in a dictionary. By organizing words by their letter at a given position,
 * it enables rapid filtering and analysis operations critical for word-guessing game strategies.
 * 
 * <p><strong>Conceptual Model:</strong><br>
 * A dictionary can be visualized as a table where:
 * <ul>
 *   <li>Each row represents a complete word</li>
 *   <li>Each column represents a specific letter position (0-indexed)</li>
 *   <li>Each cell contains a single letter</li>
 * </ul>
 * 
 * <p>This Column class represents one such vertical slice, mapping each unique letter
 * that appears in that position to the set of all words containing that letter at that position.
 * 
 * <p><strong>Example:</strong><br>
 * For position 0 (first letter) in a 5-letter word dictionary:
 * <pre>
 * 'A' → {"AROSE", "ABOUT", "ADULT", ...}
 * 'B' → {"BREAD", "BREAK", "BROWN", ...}
 * 'C' → {"CHAIR", "CREAM", "CRISP", ...}
 * </pre>
 * 
 * <p><strong>Use Cases:</strong>
 * <ul>
 *   <li>Quickly retrieve all words with a specific letter at a specific position</li>
 *   <li>Analyze letter frequency distributions for bot strategy optimization</li>
 *   <li>Filter dictionaries based on game feedback (green/amber/red responses)</li>
 *   <li>Identify the most/least common letters at each position for entropy calculations</li>
 * </ul>
 * 
 * <p><strong>Thread Safety:</strong> This class is not thread-safe. External synchronization
 * is required if instances are accessed concurrently.
 * 
 * @author Fistral Technologies
 * @see Dictionary
 * @see Response
 */
public class Column {
    
    /** 
     * Maps each character that appears in this column position to the set of all words
     * containing that character at this position. 
     */
    private final Map<Character, Set<String>> column = new HashMap<>();

    /**
     * Adds a word to this column, indexed by the specified character.
     * 
     * <p>This method associates a word with a specific letter at this column position.
     * If this is the first word containing the specified letter at this position,
     * a new entry is created in the internal map.
     * 
     * <p><strong>Example:</strong><br>
     * For column position 2, calling {@code put('O', "AROSE")} means the letter 'O'
     * appears at position 2 in the word "AROSE".
     * 
     * @param k the character at this column position in the word
     * @param word the complete word containing this character at this position
     */
    public void put(char k, String word){
        if(!column.containsKey(k)){
            column.put(k, new HashSet<>());
        }
        column.get(k).add(word);
    }

    /**
     * Retrieves all words that have the specified character at this column position.
     * 
     * <p>This method is essential for filtering operations in game strategies. For example,
     * when the game provides feedback that a specific letter must be at a specific position
     * (green/correct response), this method quickly retrieves all valid candidate words.
     * 
     * <p><strong>Example:</strong><br>
     * If this Column represents position 0 and {@code getWords('S')} is called,
     * it returns all words starting with 'S'.
     * 
     * @param c the character to look up
     * @return a Set of all words containing the specified character at this position,
     *         or {@code null} if no words contain this character at this position
     */
    public Set<String> getWords(char c){
        return column.get(c);
    }

    /**
     * Returns all unique letters that appear at this column position across all words.
     * 
     * <p>This method is useful for:
     * <ul>
     *   <li>Determining which letters are possible at this position</li>
     *   <li>Analyzing letter diversity for entropy calculations</li>
     *   <li>Implementing filtering logic based on game feedback</li>
     * </ul>
     * 
     * <p><strong>Example:</strong><br>
     * For column position 0 in a small dictionary, this might return {'A', 'B', 'C', 'S', 'T'}
     * if those are the only letters that appear as the first character in any word.
     * 
     * @return a Set of all unique characters that appear at this column position
     */
    public Set<Character> getLetters(){
        return column.keySet();
    }

    /**
     * Removes all words containing the specified letter at this column position.
     * 
     * <p>This method is used during dictionary filtering when game feedback indicates
     * that a specific letter cannot appear at this position. For example, when a guess
     * receives a "red" (not in word) or "amber" (in word, wrong position) response.
     * 
     * <p><strong>Use Case:</strong><br>
     * If the game indicates that 'E' is not at position 2, calling {@code removeLetter('E')}
     * on the Column representing position 2 eliminates all words with 'E' at that position.
     * 
     * @param c the character to remove from this column
     */
    public void removeLetter(char c){
        column.remove(c);
    }

    /**
     * Returns the number of unique letters that appear at this column position.
     * 
     * <p>This metric indicates the diversity of letters at this position and can be
     * used for:
     * <ul>
     *   <li>Measuring dictionary size reduction during filtering</li>
     *   <li>Calculating positional entropy for bot strategies</li>
     *   <li>Analyzing letter distribution patterns</li>
     * </ul>
     * 
     * @return the count of unique characters at this column position
     */
    public int length(){
        return column.size();
    }

    /**
     * Removes a specific word from this column's data structure.
     * 
     * <p>This method removes the word from the set associated with its first character.
     * It is typically called as part of a broader dictionary filtering operation where
     * individual words are eliminated based on game feedback.
     * 
     * <p><strong>Warning:</strong> This method assumes the word exists in the column
     * and uses {@code word.charAt(0)} to determine which character's set to modify.
     * Calling this on a word not in the column may result in a NullPointerException.
     * 
     * @param word the word to remove from this column
     * @throws NullPointerException if the word's first character has no associated set
     */
    public void removeWord(String word){
        column.get(word.charAt(0)).remove(word);
    }

    /**
     * Identifies the letter that appears most frequently at this column position.
     * 
     * <p>This method calculates which letter appears in the highest number of words
     * at this specific position. This information is valuable for bot strategies that
     * prioritize common letters to maximize information gain.
     * 
     * <p><strong>Algorithm:</strong> Iterates through all letters in this column and
     * counts how many words contain each letter, returning the letter with the maximum count.
     * 
     * <p><strong>Use Cases:</strong>
     * <ul>
     *   <li>Entropy-based word selection strategies</li>
     *   <li>Frequency analysis for optimal first guesses</li>
     *   <li>Statistical analysis of letter distributions</li>
     * </ul>
     * 
     * <p><strong>Example:</strong><br>
     * If at position 0:
     * <ul>
     *   <li>'S' appears in 365 words</li>
     *   <li>'C' appears in 198 words</li>
     *   <li>'A' appears in 140 words</li>
     * </ul>
     * This method returns 'S'.
     * 
     * <p><strong>Edge Cases:</strong>
     * <ul>
     *   <li>If multiple letters have the same maximum frequency, returns one arbitrarily</li>
     *   <li>If the column is empty, returns 'a' as a default value</li>
     * </ul>
     * 
     * @return the character that appears in the most words at this position,
     *         or 'a' if the column is empty
     */
    public char getMostCommonLetter(){
        char result = 'a';
        int max = 0;
        for(Map.Entry<Character,Set<String>> e: column.entrySet()){
            int next = e.getValue().size();
            if(next > max)
            {
                max = next;
                result = e.getKey();
            }
        }
        return result;
    }

    /**
     * Identifies the letter that appears least frequently at this column position.
     * 
     * <p>This method calculates which letter appears in the fewest number of words
     * at this specific position. While less commonly used than {@link #getMostCommonLetter()},
     * it can be useful for:
     * <ul>
     *   <li>Identifying rare letter combinations for advanced strategies</li>
     *   <li>Statistical analysis and dictionary characterization</li>
     *   <li>Elimination strategies that target uncommon patterns</li>
     * </ul>
     * 
     * <p><strong>Algorithm:</strong> Iterates through all letters in this column and
     * counts how many words contain each letter, returning the letter with the minimum count.
     * 
     * <p><strong>Example:</strong><br>
     * If at position 4 (last letter in 5-letter words):
     * <ul>
     *   <li>'E' appears in 422 words</li>
     *   <li>'Y' appears in 364 words</li>
     *   <li>'Q' appears in 2 words</li>
     * </ul>
     * This method returns 'Q'.
     * 
     * <p><strong>Edge Cases:</strong>
     * <ul>
     *   <li>If multiple letters have the same minimum frequency, returns one arbitrarily</li>
     *   <li>If the column is empty, returns 'a' as a default value</li>
     * </ul>
     * 
     * @return the character that appears in the fewest words at this position,
     *         or 'a' if the column is empty
     */
    public char getLeastCommonLetter(){
        char result = 'a';
        int min = 0;
        for(Map.Entry<Character,Set<String>> e: column.entrySet()){
            int next = e.getValue().size();
            if(min == 0) {
                min = next;
                result = e.getKey();
            }
            else if(next < min){
                min = next;
                result = e.getKey();
            }
        }
        return result;
    }
}
