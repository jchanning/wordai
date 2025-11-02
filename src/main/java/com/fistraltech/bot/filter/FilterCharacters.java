package com.fistraltech.bot.filter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents the set of valid characters for a single letter position in a word guess filter.
 * 
 * <p>This class maintains the constraint on which letters are still possible at a specific
 * position in the target word, based on accumulated game feedback. It starts with all letters
 * possible and progressively eliminates options as responses narrow the search space.
 * 
 * <p><strong>Conceptual Purpose:</strong><br>
 * In a word-guessing game, each position (0 to word length-1) has its own FilterCharacters
 * instance that tracks which letters could possibly appear at that position based on:
 * <ul>
 *   <li>Green responses (only one letter remains possible)</li>
 *   <li>Amber responses (letter removed from this position, but known to be elsewhere)</li>
 *   <li>Red responses (letter removed from all positions)</li>
 * </ul>
 * 
 * <p><strong>Initialization Modes:</strong>
 * <ul>
 *   <li><strong>Full Alphabet:</strong> {@code new FilterCharacters()} → All 26 letters possible</li>
 *   <li><strong>Single Letter:</strong> {@code new FilterCharacters('e')} → Only 'e' is valid 
 *       (used when a letter is confirmed via Green response)</li>
 * </ul>
 * 
 * <p><strong>Character Set:</strong><br>
 * Uses the English lowercase alphabet [a-z]. This could be extended to other character
 * sets (e.g., accented characters, symbols) for different word games or languages.
 * 
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * // Position 0 starts with all letters possible
 * FilterCharacters pos0 = new FilterCharacters();  // {a,b,c,...,z}
 * 
 * // After guess "CRANE" returns 'C' as Red (not in word)
 * pos0.remove('c');  // {a,b,d,e,f,...,z}
 * 
 * // After another guess finds 'S' is Green at position 0
 * FilterCharacters pos0Confirmed = new FilterCharacters('s');  // {s} only
 * }</pre>
 * 
 * <p><strong>Integration:</strong><br>
 * This class is used by {@link Filter} which maintains an array of FilterCharacters
 * instances, one per letter position. As responses are processed, Filter updates
 * each position's character set appropriately.
 * 
 * <p><strong>Immutability Note:</strong> While the character set is mutable through
 * remove operations, the alphabet array itself is constant.
 * 
 * <p><strong>Thread Safety:</strong> This class is not thread-safe. External
 * synchronization is required if accessed concurrently.
 * 
 * @author Fistral Technologies
 * @see Filter
 * @see com.fistraltech.core.Response
 */
public class FilterCharacters {
    private final Set<Character> filter;

    Character[] alphabet = {'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p',
            'q','r','s','t','u','v','w','x','y','z'};

    public FilterCharacters(char c){
        filter = new HashSet<>();
        filter.add(c);
    }

    public FilterCharacters(){
        filter = new HashSet<>(Arrays.asList(alphabet));
    }

    public Set<Character> getLetters(){
        return filter;
    }

    public void remove(char c){
        filter.remove(c);
    }
}
