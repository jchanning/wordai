package com.fistraltech.server.model;

import java.util.Set;
import java.util.logging.Logger;

import com.fistraltech.bot.selection.SelectionAlgo;
import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Filter;
import com.fistraltech.core.Response;
import com.fistraltech.core.WordEntropy;
import com.fistraltech.server.algo.AlgorithmRegistry;

/**
 * Mutable gameplay context for a session: filter state, suggestion strategy, and cached algorithm helpers.
 */
final class GameSessionContext {
    private static final Logger logger = Logger.getLogger(GameSessionContext.class.getName());

    private final Dictionary originalDictionary;
    private final Filter wordFilter;
    private final AlgorithmRegistry algorithmRegistry;
    private String selectedStrategy = "RANDOM";
    private WordEntropy cachedWordEntropy;
    private SelectionAlgo cachedAlgorithm;
    private String cachedAlgorithmStrategy;

    GameSessionContext(Dictionary originalDictionary, AlgorithmRegistry algorithmRegistry) {
        this.originalDictionary = originalDictionary;
        this.wordFilter = new Filter(originalDictionary.getWordLength());
        this.algorithmRegistry = algorithmRegistry;
    }

    Filter getWordFilter() {
        return wordFilter;
    }

    int getRemainingWordsCount() {
        return getFilteredDictionary().getWordCount();
    }

    int getTotalWordsCount() {
        return originalDictionary.getWordCount();
    }

    String getSelectedStrategy() {
        return selectedStrategy;
    }

    synchronized void setSelectedStrategy(String strategy) {
        this.selectedStrategy = strategy;
    }

    void setCachedWordEntropy(WordEntropy wordEntropy) {
        this.cachedWordEntropy = wordEntropy;
    }

    Dictionary getFilteredDictionary() {
        return wordFilter.apply(originalDictionary);
    }

    synchronized String suggestWord() {
        Dictionary filteredDictionary = getFilteredDictionary();
        Set<String> filteredWords = filteredDictionary.getMasterSetOfWords();

        if (filteredDictionary.getWordCount() == 0) {
            return null;
        }

        String strategyUpper = normalizeStrategy(selectedStrategy);
        boolean isUnfilteredDictionary = filteredDictionary.getWordCount() == originalDictionary.getWordCount();

        if (cachedWordEntropy != null) {
            switch (strategyUpper) {
                case "ENTROPY" -> {
                    if (!isUnfilteredDictionary) {
                        logger.fine(() -> "Using memoized WordEntropy for filtered ENTROPY suggestion");
                        return cachedWordEntropy.getMaximumEntropyWordLazy(filteredWords, filteredWords);
                    }
                    logger.fine(() -> "Using cached WordEntropy for first ENTROPY suggestion (full dict)");
                    return cachedWordEntropy.getMaximumEntropyWord(filteredWords);
                }
                case "BELLMAN_FULL_DICTIONARY" -> {
                    if (isUnfilteredDictionary) {
                        logger.fine(() -> "Using cached WordEntropy for first BELLMAN_FULL_DICTIONARY suggestion (full dict)");
                        return cachedWordEntropy.getWordWithMaximumReduction(filteredWords);
                    }
                }
                default -> {
                    // Fall through to algorithm selection below.
                }
            }
        }

        Response emptyResponse = new Response("");
        if (algorithmRegistry.isStateful(strategyUpper)) {
            SelectionAlgo algo = getCachedOrCreateAlgorithm(strategyUpper, originalDictionary);
            return algo.selectWord(emptyResponse, filteredDictionary);
        }

        SelectionAlgo algo = algorithmRegistry.create(strategyUpper, filteredDictionary);
        return algo.selectWord(emptyResponse, filteredDictionary);
    }

    private String normalizeStrategy(String strategy) {
        return algorithmRegistry.normalizeId(strategy);
    }

    private SelectionAlgo getCachedOrCreateAlgorithm(String strategy, Dictionary fullDictionary) {
        if (cachedAlgorithm != null && strategy.equals(cachedAlgorithmStrategy)) {
            logger.fine(() -> "Reusing cached algorithm instance for " + strategy);
            return cachedAlgorithm;
        }
        logger.fine(() -> "Creating new algorithm instance for " + strategy);
        cachedAlgorithm = algorithmRegistry.create(strategy, fullDictionary);
        cachedAlgorithmStrategy = strategy;
        return cachedAlgorithm;
    }
}