package com.fistraltech.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fistraltech.analysis.DictionaryAnalytics;
import com.fistraltech.analysis.WordEntropy;
import com.fistraltech.server.dto.DictionaryOption;
import com.fistraltech.util.Config;

/** 
 * Manages dictionaries used in the WordAI application. Dictionaries are loaded from a word source, typically a file. Dictionaries are 
 * static and shouldbe loaded once and cached in memory for reuse across multiple game sessions and strategies.
 * This class provides methods to create, retrieve, and manage dictionaries of different word lengths and caches them in memory.
 * It ensures efficient access to dictionaries for different game sessions and strategies.
 * Furthermore, it allows analysis of the initial dictionary characteristics using the DictionaryAnalytics to be performed once at
 * initialisation to improve performance across all sessions.
 */
public class DictionaryManager {
    private static final DictionaryManager instance = new DictionaryManager();
    
    // Cache of dictionaries keyed by word length
    private final Map<Integer, Dictionary> dictionariesByWordLength = new HashMap<>();
    
    // Cache of DictionaryAnalytics keyed by word length
    private final Map<Integer, DictionaryAnalytics> analyticsByWordLength = new HashMap<>();

    // Cache of WordEntropy keyed by word length
    private final Map<Integer, WordEntropy> entropyByWordLength = new HashMap<>();

    private DictionaryManager() {
        // Private constructor for singleton pattern
    }

    /**
     * Initialises the DictionaryManager by loading and caching dictionaries for all configured word lengths.
     * 
     * <p>This method loads dictionaries from files specified in the configuration for each word length
     * between the configured minimum and maximum. Dictionaries are cached in memory for efficient reuse
     * across multiple game sessions and strategies.
     * 
     * @param config the configuration object containing dictionary paths and word length settings
     */
    public void initialise(Config config) {
        int minWordLength = config.getMinWordLength();
        int maxWordLength = config.getMaxWordLength();
        
        // Pre-load dictionaries of common lengths if needed
        for(int length = minWordLength; length <= maxWordLength; ++length){
            try {
                
                List<DictionaryOption> availableDictionaries = config.getAvailableDictionaries();
                for(DictionaryOption dictOption : availableDictionaries){
                    if(dictOption.getWordLength() == length){
                        String dictionaryPath = dictOption.getResolvedPath();
                        Set<String> words = WordSource.getWordsFromFile(dictionaryPath);
                        
                        // Filter out words with incorrect length (e.g., header lines)
                        Set<String> validWords = new HashSet<>();
                        for (String word : words) {
                            if (word != null && word.length() == length) {
                                validWords.add(word);
                            }
                        }
                        
                        Dictionary dictionary = new Dictionary(length);
                        dictionary.addWords(validWords);
                        System.out.println("Loaded dictionary for word length " + length + " from " + dictionaryPath + " with " + validWords.size() + " words (filtered from " + words.size() + " total).");
                        dictionariesByWordLength.put(length, dictionary);
                        
                        // Pre-compute entropy for all words in this dictionary
                        System.out.println("Computing entropy values for word length " + length + "...");
                        WordEntropy wordEntropy = new WordEntropy(dictionary);
                        entropyByWordLength.put(length, wordEntropy);
                        System.out.println("Entropy computation complete for word length " + length);
                    }
                }
                // Analyse dictionary characteristics to improve performance later
                //DictionaryAnalytics.analyseDictionary(dictionary);
                //dictionariesByLength.put(length, dictionary);
            } catch (Exception e) {
                System.err.println("Failed to load dictionary for word length " + length + ": " + e.getMessage());
            }
        }

    }

    /**
     * Returns the singleton instance of the DictionaryManager.
     * 
     * @return the singleton DictionaryManager instance
     */
    public static DictionaryManager getInstance() {
        return instance;
    }

    /**
     * Retrieves a cached dictionary for the specified word length.
     * 
     * @param wordLength the length of words in the desired dictionary
     * @return the Dictionary for the specified word length, or null if no dictionary exists for that length
     */
    public Dictionary getDictionary(int wordLength) {
        return dictionariesByWordLength.get(wordLength);
    }

    /**
     * Creates and returns a DictionaryAnalytics instance for the dictionary of the specified word length.
     * 
     * <p>Note: This method creates a new DictionaryAnalytics instance on each call rather than
     * using a cached instance. Consider using the cached analytics from analyticsByWordLength
     * if performance optimization is needed.
     * 
     * @param wordLength the length of words in the dictionary to analyze
     * @return a DictionaryAnalytics instance for the specified dictionary, or null if no dictionary exists for that length
     */
    public DictionaryAnalytics getDictionaryAnalyser(int wordLength) {
        Dictionary dictionary = dictionariesByWordLength.get(wordLength);
        if (dictionary != null) {
            return new DictionaryAnalytics(dictionary);
        }
        return null;
    }

    public WordEntropy getWordEntropy(int wordLength) {
        return entropyByWordLength.get(wordLength);
    }
    
    /**
     * Retrieves the cached WordEntropy instance for a dictionary identified by its ID.
     * 
     * @param config the configuration object to resolve dictionary ID to word length
     * @param dictionaryId the dictionary identifier
     * @return the WordEntropy instance for the specified dictionary, or null if not found
     */
    public WordEntropy getWordEntropyByDictionaryId(Config config, String dictionaryId) {
        if (dictionaryId == null || dictionaryId.isEmpty()) {
            return null;
        }
        int wordLength = config.getWordLengthForDictionary(dictionaryId);
        return entropyByWordLength.get(wordLength);
    }
}
