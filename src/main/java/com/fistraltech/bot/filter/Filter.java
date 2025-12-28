package com.fistraltech.bot.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;
import com.fistraltech.core.ResponseEntry;

/**
 * Encodes and applies constraints from word guess feedback to filter valid word candidates.
 * 
 * <p>This class is the core logic engine for interpreting game responses and maintaining
 * the set of position-based letter constraints that define which words remain valid.
 * It translates color-coded feedback (Green/Amber/Red/eXcess) into dictionary filtering operations.
 * 
 * <p><strong>Status Codes:</strong>
 * <ul>
 *   <li><strong>'G' (Green):</strong> Letter is correct and in the correct position → 
 *       Remove all other letters from this position</li>
 *   <li><strong>'A' (Amber):</strong> Letter is in the word but wrong position → 
 *       Remove letter from this position, add to mustContain set</li>
 *   <li><strong>'R' (Red):</strong> Letter is not in the word at all → 
 *       Remove letter from all positions</li>
 *   <li><strong>'X' (eXcess):</strong> Letter appears in target, but this occurrence is extra → 
 *       Treat like Amber (in word, wrong position)</li>
 * </ul>
 * 
 * <p><strong>Conceptual Model:</strong><br>
 * The filter maintains a position-based character matrix:
 * <pre>
 * Initial state (5-letter word):
 * Position 0: [a-z]  (26 letters possible)
 * Position 1: [a-z]
 * Position 2: [a-z]
 * Position 3: [a-z]
 * Position 4: [a-z]
 * 
 * After guess "CRANE" → "RAARG" response:
 * Position 0: [a-z] minus 'C'  (C is red - not in word)
 * Position 1: [a-z] minus {'R', 'C'}  (R is amber - in word, wrong spot)
 * Position 2: [a-z] minus 'C'
 * Position 3: ['R'] only  (R is green - correct position!)
 * Position 4: [a-z] minus 'C'
 * mustContain: {R}  (R must appear somewhere)
 * </pre>
 * 
 * <p><strong>Duplicate Letter Handling:</strong><br>
 * The filter correctly handles words with repeated letters by tracking minimum
 * occurrence counts. If a word has 2 'E's and both are found (green/amber), the
 * filter requires at least 2 'E's in valid candidates. Excess occurrences beyond
 * what the target contains are marked 'X' and treated as amber.
 * 
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * // Initialize filter for 5-letter words
 * Filter filter = new Filter(5);
 * Dictionary dict = new Dictionary(5);
 * dict.addWordsFromFile("5-letter-words.txt");  // 2315 words
 * 
 * // Process first guess
 * Response response1 = game.guess("CRANE");  // Returns "RAARG"
 * filter.update(response1);
 * Dictionary filtered1 = filter.applyFilter(dict);  // 128 words remain
 * 
 * // Process second guess
 * Response response2 = game.guess("STARE");  // Returns "AAGGG"
 * filter.update(response2);
 * Dictionary filtered2 = filter.applyFilter(dict);  // 3 words remain
 * }</pre>
 * 
 * <p><strong>Filter Application Algorithm:</strong>
 * <ol>
 *   <li>For each word in the dictionary:</li>
 *   <li>Check if each position's letter is in that position's valid character set</li>
 *   <li>Check if the word contains all required letters (mustContain) with correct counts</li>
 *   <li>If both checks pass, include word in filtered result</li>
 * </ol>
 * 
 * <p><strong>Performance Characteristics:</strong>
 * <ul>
 *   <li>Update complexity: O(word length) per response</li>
 *   <li>Filter complexity: O(dictionary size × word length)</li>
 *   <li>Memory: O(word length × alphabet size) for position constraints</li>
 * </ul>
 * 
 * <p><strong>Thread Safety:</strong> This class is not thread-safe. Each game
 * session should maintain its own Filter instance.
 * 
 * @author Fistral Technologies
 * @see FilterCharacters
 * @see Response
 * @see Dictionary
 * @see com.fistraltech.bot.selection.SelectionAlgo
 */
public class Filter {
    private int wordLength;
    private final List<FilterCharacters> filterArray = new ArrayList<>();
    // Changed from Set to Map to track minimum occurrence count for each letter
    private final Map<Character, Integer> mustContain = new HashMap<>();

    public int getWordLength() {
        return wordLength;
    }

    /* Create a filter object of the correct word length */
    public Filter(int wordLength) {
        if (wordLength <= 0) {
            throw new IllegalArgumentException("wordLength must be positive");
        }
        this.wordLength = wordLength;
        for(int i =0; i<wordLength; ++i){
            filterArray.add(new FilterCharacters());
        }
    }
    
    /* Update the filter based on the response object returned  */
    public void update(Response response){
        List<ResponseEntry> status = response.getStatuses();
        
        // Count minimum required occurrences of each letter based on THIS response
        // Green and Amber indicate the letter exists in the target
        Map<Character, Integer> responseLetterCounts = new HashMap<>();
        for(int i=0; i<status.size(); ++i){
            ResponseEntry re = status.get(i);
            if(re.status == 'G' || re.status == 'A'){
                responseLetterCounts.put(re.letter, 
                responseLetterCounts.getOrDefault(re.letter, 0) + 1);
            }
        }
        
        // Update mustContain with the MAXIMUM required count seen across all responses
        for(Map.Entry<Character, Integer> entry : responseLetterCounts.entrySet()){
            char letter = entry.getKey();
            int count = entry.getValue();
            mustContain.put(letter, Math.max(mustContain.getOrDefault(letter, 0), count));
        }
        
        // First pass: process all Green letters
        for(int i=0; i<status.size(); ++i){
            ResponseEntry re = status.get(i);
            if(re.status == 'G'){
                filterArray.set(i, new FilterCharacters(re.letter));
            }
        }
        
        // Second pass: process Amber, Red, and eXcess letters
        // Build map of letters that have Green or Amber status (exist in target) 
        // or eXcess (exists, but not in this position)
        Map<Character, Boolean> hasNonRedStatus = new HashMap<>();
        for(int i=0; i<status.size(); ++i){
            ResponseEntry re = status.get(i);
            if(re.status == 'G' || re.status == 'A'){
                hasNonRedStatus.put(re.letter, true);
            }
        }
        
        for(int i=0; i<status.size(); ++i){
            ResponseEntry re = status.get(i);
            if (null != re.status)switch (re.status) {
                case 'R':
                    // Only remove letter entirely if it has no Green or Amber instances
                    if(!hasNonRedStatus.containsKey(re.letter)){
                        removeLetter(re.letter);
                    }   break;
                case 'A':
                    // Remove from this position only
                    filterArray.get(i).remove(re.letter);
                    break;
            // X means excess - letter exists but this is an extra instance
            // Don't remove the letter from all positions, just ignore this instance
            // No action needed - the letter will be handled by its G/A instances
                case 'X':
                    // Remove from this position only
                    filterArray.get(i).remove(re.letter);
                    break;
                default:
                    break;
            }
        }
    }

    /** Remove this letter because it does not exist in the word*/
    public void removeLetter(char letter){
        for(FilterCharacters fc : filterArray){
            // Don't remove if this is the only letter left (locked by Green status)
            if(fc.getLetters().size() > 1) {
                fc.remove(letter);
            }
        }
    }

    /** This letter exists in the word, but not in this position*/
    public void removeLetter(char letter, int i){
        filterArray.get(i).remove(letter);
    }

    /** This letter exists in the word in this position*/
    public void removeAllOtherLetters(char letter, int position){
        filterArray.set(position, new FilterCharacters(letter));
    }

    /** Applies the Filter to the input Dictionary and returns a new Dictionary containing only words that are valid
     * and therefore passed through the filter*/
    public Dictionary apply(Dictionary input){
        // Filter words based on the filter criteria
        Set<String> filteredWords = input.getWords(filterArray);
        
        Set<String> result = new TreeSet<>();
      
        // Remove any words that do not contain the required count of each letter
        for(String word : filteredWords){
            boolean valid = true;
            for(Map.Entry<Character, Integer> entry : mustContain.entrySet()){
                char letter = entry.getKey();
                int requiredCount = entry.getValue();
                
                // Count occurrences of this letter in the word
                int actualCount = 0;
                for(int i = 0; i < word.length(); i++){
                    if(word.charAt(i) == letter){
                        actualCount++;
                    }
                }
                
                // Word must have at least the required count
                if(actualCount < requiredCount) {
                    valid = false;
                    break;
                }
            }
            if(valid)
            {
                result.add(word);
            }
        }
        Dictionary d = new Dictionary(wordLength);
            d.addWords(result);

        return d;
    }
    
    /**
     * Clears all filter constraints to reset for a new game.
     * Resets all position filters and clears the mustContain set.
     */
    public void clear() {
        filterArray.clear();
        for(int i = 0; i < wordLength; ++i){
            filterArray.add(new FilterCharacters());
        }
        mustContain.clear();
    }}