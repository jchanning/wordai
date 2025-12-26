package com.fistraltech.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fistraltech.analysis.DictionaryAnalytics;
import com.fistraltech.analysis.WordEntropy;
import com.fistraltech.server.dto.DictionaryOption;
import com.fistraltech.util.Config;

/** 
 * Manages dictionaries used in the WordAI application. Dictionaries are loaded from a word source, typically a file.
 * This class provides methods to create, retrieve, and manage dictionaries of various word lengths and caches them in memory.
 * It ensures efficient access to dictionaries for different game sessions and strategies.
 * Furthermore, it allows analysis of dictionary characteristics using the DictionaryAnalytics to be performed on initialisation to improve performance.
 */
public class DictionaryManager {
    private static final DictionaryManager instance = new DictionaryManager();
    private final Map<Integer, Dictionary> dictionariesByWordLength = new HashMap<>();
    
    //Cache data for the initial dictionaries
    private final Map<Integer, DictionaryAnalytics> analyticsByWordLength = new HashMap<>();
    private final Map<Integer, WordEntropy> entropyByWordLength = new HashMap<>();

    private static int MIN_WORD_LENGTH = 4;
    private static int MAX_WORD_LENGTH = 7;

    private DictionaryManager() {
        // Private constructor for singleton pattern
    }

    public void initialise(Config config) {
        // Pre-load dictionaries of common lengths if needed
        for(int length = MIN_WORD_LENGTH; length <= MAX_WORD_LENGTH; ++length){
            try {
                
                List<DictionaryOption> availableDictionaries = config.getAvailableDictionaries();
                for(DictionaryOption dictOption : availableDictionaries){
                    if(dictOption.getWordLength() == length){
                        String dictionaryPath = dictOption.getResolvedPath();
                        Set<String> words = WordSource.getWordsFromFile(dictionaryPath);
                        Dictionary dictionary = new Dictionary(length);
                        dictionary.addWords(words);
                        System.out.println("Loaded dictionary for word length " + length + " from " + dictionaryPath + " with " + words.size() + " words.");
                        dictionariesByWordLength.put(length, dictionary);
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

    public static DictionaryManager getInstance() {
        return instance;
    }

    public Dictionary getDictionary(int wordLength) {
        return dictionariesByWordLength.get(wordLength);
    }

    public DictionaryAnalytics gDictionaryAnalyser(int wordLength) {
        Dictionary dictionary = dictionariesByWordLength.get(wordLength);
        if (dictionary != null) {
            return new DictionaryAnalytics(dictionary);
        }
        return null;
    }
}
