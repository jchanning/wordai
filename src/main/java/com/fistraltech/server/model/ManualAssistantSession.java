package com.fistraltech.server.model;

import java.util.ArrayList;
import java.util.List;

import com.fistraltech.bot.selection.SelectionAlgo;
import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Filter;
import com.fistraltech.core.Response;
import com.fistraltech.server.algo.AlgorithmRegistry;

/**
 * In-memory assistant session for target-free manual Wordle guidance.
 */
public class ManualAssistantSession {

    private final String sessionId;
    private final String dictionaryId;
    private final Dictionary masterDictionary;
    private final Filter wordFilter;
    private final AlgorithmRegistry algorithmRegistry;
    private final List<Response> feedbackHistory = new ArrayList<>();

    private String selectedStrategy;
    private SelectionAlgo cachedAlgorithm;
    private String cachedAlgorithmStrategy;

    public ManualAssistantSession(String sessionId,
                                  String dictionaryId,
                                  Dictionary masterDictionary,
                                  AlgorithmRegistry algorithmRegistry,
                                  String selectedStrategy) {
        this.sessionId = sessionId;
        this.dictionaryId = dictionaryId;
        this.masterDictionary = masterDictionary;
        this.algorithmRegistry = algorithmRegistry;
        this.selectedStrategy = algorithmRegistry.normalizeId(selectedStrategy);
        this.wordFilter = new Filter(masterDictionary.getWordLength());
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getDictionaryId() {
        return dictionaryId;
    }

    public int getWordLength() {
        return masterDictionary.getWordLength();
    }

    public synchronized String getSelectedStrategy() {
        return selectedStrategy;
    }

    public synchronized void setSelectedStrategy(String strategy) {
        this.selectedStrategy = algorithmRegistry.normalizeId(strategy);
        this.cachedAlgorithm = null;
        this.cachedAlgorithmStrategy = null;
    }

    public synchronized int getAttemptCount() {
        return feedbackHistory.size();
    }

    public synchronized void applyFeedback(Response response) {
        wordFilter.update(response);
        feedbackHistory.add(response);
    }

    public synchronized int getRemainingWordsCount() {
        return getFilteredDictionary().getWordCount();
    }

    public synchronized Dictionary getFilteredDictionary() {
        return wordFilter.apply(masterDictionary);
    }

    public synchronized String suggestWord() {
        Dictionary filteredDictionary = getFilteredDictionary();
        if (filteredDictionary.getWordCount() == 0) {
            return null;
        }

        String normalizedStrategy = algorithmRegistry.normalizeId(selectedStrategy);
        Response lastResponse = feedbackHistory.isEmpty()
            ? new Response("")
            : feedbackHistory.get(feedbackHistory.size() - 1);

        SelectionAlgo algorithm;
        if (algorithmRegistry.isStateful(normalizedStrategy)) {
            algorithm = getCachedOrCreateAlgorithm(normalizedStrategy);
            return algorithm.selectWord(lastResponse, filteredDictionary);
        }

        algorithm = algorithmRegistry.create(normalizedStrategy, filteredDictionary);
        return algorithm.selectWord(lastResponse, filteredDictionary);
    }

    private SelectionAlgo getCachedOrCreateAlgorithm(String strategy) {
        if (cachedAlgorithm != null && strategy.equals(cachedAlgorithmStrategy)) {
            return cachedAlgorithm;
        }

        cachedAlgorithm = algorithmRegistry.create(strategy, masterDictionary);
        cachedAlgorithmStrategy = strategy;
        return cachedAlgorithm;
    }
}