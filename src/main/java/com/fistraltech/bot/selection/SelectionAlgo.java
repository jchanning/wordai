package com.fistraltech.bot.selection;

import java.util.Objects;

import com.fistraltech.bot.filter.Filter;
import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;

/**
 * Abstract base class for word selection algorithms in automated game play.
 * 
 * <p>This class defines the framework for implementing different strategies to choose
 * the next word to guess in a word-guessing game. Subclasses implement specific selection
 * logic (random, frequency-based, entropy-maximizing, etc.) while this base class handles
 * common operations like filter management and dictionary updating.
 * 
 * <p><strong>Algorithm Lifecycle:</strong>
 * <ol>
 *   <li><strong>Initialization:</strong> Construct with full dictionary</li>
 *   <li><strong>Selection:</strong> {@link #selectWord(Response)} called to choose next guess</li>
 *   <li><strong>Filter Update:</strong> Automatically applies game response to filter</li>
 *   <li><strong>Dictionary Update:</strong> Reduces valid word set based on new constraints</li>
 *   <li><strong>Repeat:</strong> Until word is found or max attempts reached</li>
 * </ol>
 * 
 * <p><strong>Template Method Pattern:</strong><br>
 * This class uses the Template Method pattern:
 * <ul>
 *   <li>{@link #selectWord(Response)} - Public template method (manages filtering)</li>
 *   <li>{@link #selectWord(Response, Dictionary)} - Abstract method (subclass strategy)</li>
 * </ul>
 * 
 * <p><strong>Built-in Selection Strategies:</strong>
 * <ul>
 *   <li><strong>{@code SelectRandom}:</strong> Random selection from valid words
 *       <ul><li>Simple, baseline strategy</li><li>Average ~5-6 attempts</li></ul>
 *   </li>
 *   <li><strong>{@code SelectMaximumEntropy}:</strong> Information theory-based selection
 *       <ul><li>Maximizes information gain</li><li>Average ~3-4 attempts</li><li>Computationally intensive</li></ul>
 *   </li>
 *   <li><strong>{@code SelectBellmanFullDictionary}:</strong> Uses full dictionary guesses
 *       <ul><li>Includes known incorrect words to reduce remaining possibilities</li></ul>
 *   </li>
 * </ul>
 * 
 * <p><strong>Implementation Guide:</strong><br>
 * To create a new selection algorithm:
 * <pre>{@code
 * public class SelectMyStrategy extends SelectionAlgo {
 *     public SelectMyStrategy(Dictionary dictionary) {
 *         super(dictionary);
 *         setAlgoName("MyStrategy");
 *     }
 *     
 *     @Override
 *     protected String selectWord(Response lastResponse, Dictionary filteredDict) {
 *         // Implement your selection logic here
 *         // filteredDict contains only valid words based on all previous responses
 *         return chosenWord;
 *     }
 * }
 * }</pre>
 * 
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * Dictionary dict = new Dictionary(5);
 * dict.addWordsFromFile("5-letter-words.txt");
 * 
 * SelectionAlgo algorithm = new SelectMaximumEntropy(dict);
 * Response response = new Response("");  // Empty for first guess
 * 
 * while (!response.getWinner()) {
 *     String nextWord = algorithm.selectWord(response);
 *     response = game.guess(nextWord);
 * }
 * }</pre>
 * 
 * <p><strong>Performance Considerations:</strong>
 * <ul>
 *   <li>Filter application is O(dictionary size × word length)</li>
 *   <li>Selection logic complexity varies by implementation</li>
 *   <li>Consider caching for expensive calculations</li>
 * </ul>
 * 
 * <p><strong>Thread Safety:</strong> This class and its subclasses are not thread-safe.
 * Each game session should use its own algorithm instance.
 * 
 * @author Fistral Technologies
 * @see Filter
 * @see Dictionary
 * @see com.fistraltech.bot.WordGamePlayer
 */
public abstract class SelectionAlgo {
    private final Filter filter;
    // The dictionary of all words at the start of the game
    private final Dictionary dictionary;

    // The dictionary of words that remain valid after processing the responses and filtering out words that do not match the last response
    private Dictionary updatedDictionary;

    // Name of the selection algorithm
    private String algoName;

    public SelectionAlgo(Dictionary dictionary) {
        this.dictionary = dictionary;
        this.filter = new Filter(dictionary.getWordLength());
    }

    public Dictionary getUpdatedDictionary() {
        return updatedDictionary;
    }

    public String getAlgoName() {
        return algoName;
    }

    public void setAlgoName(String algoName) {
        this.algoName = algoName;
    }

    /**
     * Selects the next word to be guessed based on the last response.
     * This method applies the filter to update the dictionary before
     * delegating to the subclass-specific selection logic.
     *
     * @param lastResponse the last response received
     * @return the next word to be guessed
     */
    public String selectWord(Response lastResponse){
        applyFilter(lastResponse);
        return selectWord(lastResponse, updatedDictionary);
    }

    /**
     * Updates the filter with the last response and applies the filter to the dictionary to get the words that remain valid.
     *
     * @param lastResponse the last response received
     */
    private void applyFilter(Response lastResponse) {
        // If the last response word is empty, use the original dictionary
        if (Objects.equals(lastResponse.getWord(), "")) {
            updatedDictionary = dictionary;
        } else {
            // Update the filter with the last response and apply it to the ORIGINAL dictionary - probably not optimal, needs to be 
            // evaluated.
            // The filter accumulates all constraints from all responses
            filter.update(lastResponse);
            updatedDictionary = filter.apply(dictionary);
        }
    }

    /**
     * Selects the next word to be guessed based on the last response and the current dictionary.
     *
     * @param lastResponse the last response received
     * @param dictionary the current dictionary of words
     * @return the next word to be guessed
     */
    abstract String selectWord(Response lastResponse, Dictionary dictionary);

    /**
     * Resets the algorithm state for a new game.
     * Clears the filter and resets the updated dictionary.
     */
    public void reset() {
        filter.clear();
        updatedDictionary = dictionary;
    }

}
