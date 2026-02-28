package com.fistraltech.bot;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fistraltech.bot.filter.Filter;
import com.fistraltech.bot.selection.SelectionAlgo;
import com.fistraltech.core.Dictionary;
import com.fistraltech.core.InvalidWordException;
import com.fistraltech.core.Response;
import com.fistraltech.core.WordGame;

/**
 * Automated player implementation for word-guessing games using pluggable strategies.
 *
 * <p>This class implements the {@link Player} interface to provide an AI bot that can
 * play word-guessing games automatically. The bot uses a configurable {@link SelectionAlgo}
 * strategy to choose words intelligently based on game feedback.
 *
 * <p><strong>Architecture:</strong>
 * <ul>
 *   <li><strong>Strategy Pattern:</strong> Uses pluggable {@link SelectionAlgo} implementations</li>
 *   <li><strong>Filter-based Logic:</strong> Owns a {@link Filter} to track valid words per game</li>
 *   <li><strong>History Tracking:</strong> Records dictionary states and responses for analysis</li>
 * </ul>
 *
 * <p><strong>Game Loop:</strong>
 * <pre>
 * 1. Apply filter to produce the current candidate dictionary
 * 2. Select a word using the configured algorithm
 * 3. Submit the guess to the game
 * 4. Receive and record the response
 * 5. Repeat until the word is found or max attempts reached
 * </pre>
 *
 * <p><strong>Thread Safety:</strong> not thread-safe; use one instance per game thread.
 *
 * @author Fistral Technologies
 * @see Player
 * @see SelectionAlgo
 * @see Filter
 * @see GameAnalytics
 */
public class WordGamePlayer implements Player {
    private static final Logger logger = Logger.getLogger(WordGamePlayer.class.getName());
    private final Dictionary dictionary;
    private final SelectionAlgo algo;
    private final Filter filter;

    private WordGame wordGame;

    @Override
    public Dictionary getDictionary() {
        return dictionary;
    }

    @Override
    public SelectionAlgo getAlgo() {
        return algo;
    }

    @Override
    public WordGame getWordGame() {
        return wordGame;
    }

    private final DictionaryHistory dictionaryHistory = new DictionaryHistory();
    private final ResultHistory resultHistory = new ResultHistory();

    public WordGamePlayer(WordGame wordGame, SelectionAlgo algo){
        this.wordGame = wordGame;
        this.dictionary = wordGame.getDictionary();
        this.algo = algo;
        this.filter = new Filter(dictionary.getWordLength());
    }

    public void setWordGame(WordGame wordGame){
        this.wordGame = wordGame;
    }

    /** Plays a single randomly selected game */
    @Override
    public void playGame(WordGame wg){
        // Reset filter state at the start of each game so previous game constraints
        // do not bleed into the next game.
        filter.clear();

        try {
            /* For the first guess, you do not have a result yet, so the word is an empty String */
            Response response = new Response("");

            while(!response.getWinner()){
                // Determine the current candidate dictionary based on accumulated feedback.
                Dictionary filteredDictionary;
                if (Objects.equals(response.getWord(), "")) {
                    filteredDictionary = dictionary;
                } else {
                    filter.update(response);
                    filteredDictionary = filter.apply(dictionary);
                }

                String selectedWord = algo.selectWord(response, filteredDictionary);
                response = wg.guess(selectedWord);

                resultHistory.add(response);
                dictionaryHistory.add(filteredDictionary);
            }

        }
        catch(InvalidWordException e){
            logger.log(Level.SEVERE, "Error playing game", e);
        }
    }

    @Override
    public DictionaryHistory getDictionaryHistory() {
        return dictionaryHistory;
    }

    @Override
    public ResultHistory getResultHistory() {
        return resultHistory;
    }

}
