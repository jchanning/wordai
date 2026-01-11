package com.fistraltech.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.fistraltech.analysis.WordEntropy;
import com.fistraltech.core.Dictionary;
import com.fistraltech.core.WordSource;
import com.fistraltech.server.dto.DictionaryOption;
import com.fistraltech.util.Config;
import com.fistraltech.util.ConfigManager;

import jakarta.annotation.PostConstruct;

/**
 * Spring-managed service for dictionary management and caching.
 * 
 * <p>This service consolidates all dictionary-related functionality:
 * <ul>
 *   <li>Loading dictionaries from files at application startup</li>
 *   <li>Caching dictionaries by ID and word length for fast access</li>
 *   <li>Pre-computing and caching WordEntropy (response buckets, entropy values)</li>
 *   <li>Providing dictionary copies for game sessions (so filtering doesn't affect master)</li>
 * </ul>
 * 
 * <p><strong>Performance benefits:</strong>
 * <ul>
 *   <li>Dictionaries loaded once at startup, no file I/O during game creation</li>
 *   <li>Response buckets computed once, shared read-only across all games</li>
 *   <li>Entropy values pre-computed for instant bot decision making</li>
 * </ul>
 * 
 * @author Fistral Technologies
 */
@Service
public class DictionaryService {
    private static final Logger logger = Logger.getLogger(DictionaryService.class.getName());
    
    // Master dictionaries by dictionary ID (e.g., "default", "easy", "hard", "expert")
    private final Map<String, Dictionary> dictionariesById = new HashMap<>();
    
    // Master dictionaries by word length for quick lookup
    private final Map<Integer, Dictionary> dictionariesByLength = new HashMap<>();
    
    // Pre-computed WordEntropy by dictionary ID
    private final Map<String, WordEntropy> entropyById = new HashMap<>();
    
    // Pre-computed WordEntropy by word length
    private final Map<Integer, WordEntropy> entropyByLength = new HashMap<>();
    
    // Configuration
    private Config config;
    private boolean initialized = false;
    
    /**
     * Initializes all dictionaries and pre-computes entropy values at application startup.
     * This method runs automatically after the service is constructed.
     */
    @PostConstruct
    public void initialize() {
        long startTime = System.currentTimeMillis();
        logger.info("DictionaryService initialization starting...");
        
        try {
            this.config = ConfigManager.getInstance().createGameConfig();
            List<DictionaryOption> availableDictionaries = config.getAvailableDictionaries();
            
            for (DictionaryOption dictOption : availableDictionaries) {
                try {
                    loadAndCacheDictionary(dictOption);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to load dictionary: " + dictOption.getId(), e);
                }
            }
            
            initialized = true;
            long elapsed = System.currentTimeMillis() - startTime;
            logger.info(String.format("DictionaryService initialization complete in %d ms. Loaded %d dictionaries.", 
                       elapsed, dictionariesById.size()));
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "DictionaryService initialization failed", e);
            throw new RuntimeException("Failed to initialize DictionaryService", e);
        }
    }
    
    /**
     * Loads a dictionary from file and caches it along with pre-computed entropy values.
     */
    private void loadAndCacheDictionary(DictionaryOption dictOption) throws IOException {
        String dictionaryId = dictOption.getId();
        int wordLength = dictOption.getWordLength();
        String dictionaryPath = dictOption.getResolvedPath();
        
        logger.info(String.format("Loading dictionary '%s' (length=%d) from: %s", 
                   dictionaryId, wordLength, dictionaryPath));
        
        // Load words from file
        Set<String> allWords = WordSource.getWordsFromFile(dictionaryPath);
        
        // Filter to only valid words of correct length
        Set<String> validWords = new HashSet<>();
        for (String word : allWords) {
            if (word != null && word.length() == wordLength) {
                validWords.add(word);
            }
        }
        
        // Create and populate dictionary
        Dictionary dictionary = new Dictionary(wordLength);
        dictionary.addWords(validWords);
        
        logger.info(String.format("Loaded dictionary '%s' with %d words (filtered from %d total)", 
                   dictionaryId, validWords.size(), allWords.size()));
        
        // Cache by ID and word length
        dictionariesById.put(dictionaryId, dictionary);
        dictionariesByLength.put(wordLength, dictionary);
        
        // Pre-compute entropy values (expensive but done once)
        logger.info(String.format("Computing entropy values for dictionary '%s'...", dictionaryId));
        long entropyStart = System.currentTimeMillis();
        
        // Use the already-loaded config to avoid redundant ConfigManager calls
        WordEntropy wordEntropy = new WordEntropy(dictionary, config);
        
        long entropyElapsed = System.currentTimeMillis() - entropyStart;
        logger.info(String.format("Entropy computation for '%s' complete in %d ms", 
                   dictionaryId, entropyElapsed));
        
        // Cache entropy by ID and word length
        entropyById.put(dictionaryId, wordEntropy);
        entropyByLength.put(wordLength, wordEntropy);
    }
    
    /**
     * Returns a fresh copy of the dictionary for use in a game session.
     * 
     * <p>The returned dictionary is a clone of the master, so filtering operations
     * during gameplay will not affect the cached master dictionary.
     * 
     * @param dictionaryId the dictionary identifier (e.g., "default", "easy")
     * @return a cloned Dictionary for game use, or null if not found
     */
    public Dictionary getDictionaryForGame(String dictionaryId) {
        Dictionary master = getMasterDictionary(dictionaryId);
        if (master == null) {
            return null;
        }
        return master.clone();
    }
    
    /**
     * Returns a fresh copy of the dictionary for use in a game session, by word length.
     * 
     * @param wordLength the word length
     * @return a cloned Dictionary for game use, or null if not found
     */
    public Dictionary getDictionaryForGameByLength(int wordLength) {
        Dictionary master = getMasterDictionaryByLength(wordLength);
        if (master == null) {
            return null;
        }
        return master.clone();
    }
    
    /**
     * Returns the read-only master dictionary for analysis or display.
     * 
     * <p><strong>Warning:</strong> Do not modify this dictionary. For game sessions
     * that require filtering, use {@link #getDictionaryForGame(String)} instead.
     * 
     * @param dictionaryId the dictionary identifier
     * @return the master Dictionary, or null if not found
     */
    public Dictionary getMasterDictionary(String dictionaryId) {
        if (dictionaryId == null || dictionaryId.isEmpty()) {
            dictionaryId = "default";
        }
        return dictionariesById.get(dictionaryId);
    }
    
    /**
     * Returns the read-only master dictionary by word length.
     * 
     * @param wordLength the word length
     * @return the master Dictionary, or null if not found
     */
    public Dictionary getMasterDictionaryByLength(int wordLength) {
        return dictionariesByLength.get(wordLength);
    }
    
    /**
     * Returns the pre-computed WordEntropy for a dictionary.
     * 
     * <p>This provides instant access to:
     * <ul>
     *   <li>Entropy values for each word</li>
     *   <li>Response buckets for each word</li>
     *   <li>Maximum entropy word (best first guess)</li>
     * </ul>
     * 
     * @param dictionaryId the dictionary identifier
     * @return the cached WordEntropy, or null if not found
     */
    public WordEntropy getWordEntropy(String dictionaryId) {
        if (dictionaryId == null || dictionaryId.isEmpty()) {
            dictionaryId = "default";
        }
        return entropyById.get(dictionaryId);
    }
    
    /**
     * Returns the pre-computed WordEntropy by word length.
     * 
     * @param wordLength the word length
     * @return the cached WordEntropy, or null if not found
     */
    public WordEntropy getWordEntropyByLength(int wordLength) {
        return entropyByLength.get(wordLength);
    }
    
    /**
     * Returns the list of available dictionary options.
     * 
     * @return list of DictionaryOption objects
     */
    public List<DictionaryOption> getAvailableDictionaries() {
        return config.getAvailableDictionaries();
    }
    
    /**
     * Gets the word length for a given dictionary ID.
     * 
     * @param dictionaryId the dictionary identifier
     * @return the word length, or -1 if not found
     */
    public int getWordLengthForDictionary(String dictionaryId) {
        Dictionary dict = getMasterDictionary(dictionaryId);
        return dict != null ? dict.getWordLength() : -1;
    }
    
    /**
     * Gets the default configuration.
     * 
     * @return the Config object
     */
    public Config getConfig() {
        return config;
    }
    
    /**
     * Checks if the service has been fully initialized.
     * 
     * @return true if initialization is complete
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Returns the number of cached dictionaries.
     * 
     * @return dictionary count
     */
    public int getDictionaryCount() {
        return dictionariesById.size();
    }
}
