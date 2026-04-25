package com.fistraltech.server.model;

import java.util.logging.Logger;

import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Filter;
import com.fistraltech.core.WordEntropy;
import com.fistraltech.core.WordGame;
import com.fistraltech.server.algo.AlgorithmRegistry;
import com.fistraltech.util.Config;

/**
 * In-memory representation of a single active game.
 *
 * <p>A {@link GameSession} binds together:
 * <ul>
 *   <li>the active {@link WordGame} instance (target word + guess history)</li>
 *   <li>a cumulative {@link Filter} that encodes constraints derived from guesses</li>
 *   <li>the original dictionary/config context used to create the game</li>
 * </ul>
 *
 * <p><strong>Lifecycle</strong>
 * <ol>
 *   <li>Created by {@link com.fistraltech.server.WordGameService#createGame(String, Integer, String)}.</li>
 *   <li>Mutated by guesses via {@link com.fistraltech.server.WordGameService#makeGuess(String, String)}.</li>
 *   <li>Deleted via {@link com.fistraltech.server.WordGameService#removeGameSession(String)}.</li>
 * </ol>
 *
 * <p><strong>Strategy</strong>
 * <ul>
 *   <li>The API stores a strategy id string (default {@code RANDOM}).</li>
 *   <li>{@link #suggestWord()} uses the current strategy to propose a next guess based on the filtered dictionary.</li>
 * </ul>
 *
 * <p><strong>Thread safety</strong>: {@link #suggestWord()} and {@link #setSelectedStrategy(String)}
 * are {@code synchronized} on {@code this}.  Combined with the {@code synchronized(session)} block
 * in {@link com.fistraltech.server.WordGameService#makeGuess}, all mutating operations on a session
 * use the same intrinsic lock, preventing concurrent guess / suggestion / strategy-change races.
 *
 * @author Fistral Technologies
 */
public class GameSession {
    private static final Logger logger = Logger.getLogger(GameSession.class.getName());
    
    private final String gameId;
    private final WordGame wordGame;
    private final Config config;
    private final GameSessionContext context;
    private final GameSessionMetadata metadata;
    
    public GameSession(String gameId, WordGame wordGame, Config config, Dictionary dictionary,
                       AlgorithmRegistry algorithmRegistry) {
        this.gameId = gameId;
        this.wordGame = wordGame;
        this.config = config;
        this.context = new GameSessionContext(dictionary, algorithmRegistry);
        this.metadata = new GameSessionMetadata();
    }
    
    public String getGameId() {
        return gameId;
    }
    
    public WordGame getWordGame() {
        return wordGame;
    }
    
    public Config getConfig() {
        return config;
    }
    
    public boolean isGameEnded() {
        return metadata.isGameEnded();
    }
    
    public void setGameEnded(boolean gameEnded) {
        metadata.setGameEnded(gameEnded);
    }

    public String getDictionaryId() {
        return metadata.getDictionaryId();
    }

    public void setDictionaryId(String dictionaryId) {
        metadata.setDictionaryId(dictionaryId);
    }

    public Long getUserId() {
        return metadata.getUserId();
    }

    public void setUserId(Long userId) {
        metadata.setUserId(userId);
    }

    public String getBrowserSessionId() {
        return metadata.getBrowserSessionId();
    }

    public void setBrowserSessionId(String browserSessionId) {
        metadata.setBrowserSessionId(browserSessionId);
    }

    public int getCurrentAttempts() {
        return wordGame.getNoOfAttempts();
    }
    
    public int getMaxAttempts() {
        return config.getMaxAttempts();
    }
    
    public boolean isMaxAttemptsReached() {
        return getCurrentAttempts() >= getMaxAttempts();
    }
    
    public Filter getWordFilter() {
        return context.getWordFilter();
    }
    
    public int getRemainingWordsCount() {
        return context.getRemainingWordsCount();
    }
    
    public int getTotalWordsCount() {
        return context.getTotalWordsCount();
    }
    
    public String getSelectedStrategy() {
        return context.getSelectedStrategy();
    }
    
    public synchronized void setSelectedStrategy(String strategy) {
        context.setSelectedStrategy(strategy);
    }
    
    /**
     * Sets the cached WordEntropy from DictionaryService.
     * This enables fast entropy-based suggestions using pre-computed values.
     * 
     * @param wordEntropy the pre-computed WordEntropy instance
     */
    public void setCachedWordEntropy(WordEntropy wordEntropy) {
        context.setCachedWordEntropy(wordEntropy);
    }
    
    /**
     * Gets the current filtered dictionary based on all guesses made so far.
     */
    public Dictionary getFilteredDictionary() {
        return context.getFilteredDictionary();
    }
    
    /**
     * Suggests a next word based on {@link #selectedStrategy}.
     *
     * <p>For ENTROPY strategy:
     * <ul>
     *   <li>First guess (unfiltered dictionary): uses pre-computed cached values</li>
     *   <li>Subsequent guesses (filtered dictionary): recomputes values based on remaining words</li>
     * </ul>
     *
     * <p><strong>Why recomputation is required:</strong> Entropy measures
     * how a candidate word partitions the <em>current</em> dictionary into response buckets.
     * When the dictionary changes (words are filtered out after each guess), the bucket
     * distributions change, so the metric values must be recomputed. Cached values are only
     * valid for the original unfiltered dictionary.
     *
     * @return a suggested word, or {@code null} if no valid words remain
     */
    public synchronized String suggestWord() {
        logger.fine(() -> "Delegating suggestion generation for session " + gameId);
        return context.suggestWord();
    }
}