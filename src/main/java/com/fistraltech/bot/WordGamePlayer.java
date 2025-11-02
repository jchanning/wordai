package com.fistraltech.bot;

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
 *   <li><strong>Filter-based Logic:</strong> Maintains a {@link Filter} to track valid words</li>
 *   <li><strong>History Tracking:</strong> Records dictionary states and responses for analysis</li>
 * </ul>
 * 
 * <p><strong>Game Loop:</strong><br>
 * The {@link #playGame(WordGame)} method implements the core game loop:
 * <pre>
 * 1. Select a word using the configured algorithm
 * 2. Submit the guess to the game
 * 3. Receive and record the response
 * 4. Update the filter based on feedback
 * 5. Repeat until the word is found
 * </pre>
 * 
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * // Create a dictionary and game
 * Dictionary dict = new Dictionary(5);
 * dict.addWordsFromFile("5-letter-words.txt");
 * WordGame game = new WordGame(dict, dict, config);
 * 
 * // Create a player with a selection strategy
 * SelectionAlgo strategy = new SelectMaximumEntropy(dict);
 * WordGamePlayer player = new WordGamePlayer(game, strategy);
 * 
 * // Play the game
 * player.playGame(game);
 * }</pre>
 * 
 * <p><strong>Available Strategies:</strong>
 * <ul>
 *   <li>{@code SelectRandom} - Random word selection</li>
 *   <li>{@code SelectMostCommonLetters} - Frequency-based selection</li>
 *   <li>{@code SelectMaximumEntropy} - Information theory-based selection</li>
 *   <li>{@code SelectFixedFirstWord} - Uses a predefined optimal first word</li>
 * </ul>
 * 
 * <p><strong>Performance Tracking:</strong><br>
 * History objects track the player's performance:
 * <ul>
 *   <li>{@link DictionaryHistory} - How quickly the valid word set shrinks</li>
 *   <li>{@link ResultHistory} - Each guess and its response</li>
 * </ul>
 * 
 * @author Fistral Technologies
 * @see Player
 * @see SelectionAlgo
 * @see Filter
 * @see GameAnalytics
 */
public class WordGamePlayer implements Player {
    private final Dictionary dictionary;
    private final SelectionAlgo algo;

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
    }

    public void setWordGame(WordGame wordGame){
        this.wordGame = wordGame;
    }

    public void play(){

    }

    /** Plays a single randomly selected game*/
    @Override
    public void playGame(WordGame wg){
        try {
            Filter filter = new Filter(dictionary.getWordLength());
            
            /* For the first guess, you do not have a result yet, so the word is an empty String*/
            Response response = new Response("");

            while(!response.getWinner()){
                String selectedWord = algo.selectWord(response);
                //System.out.println("The algo selected: " + selectedWord + " from " + algo.getUpdatedDictionary().getWordCount() + " words.");
                
                response = wg.guess(selectedWord);

                // CommandLineGame.printResult(response); // Removed - CommandLineGame class doesn't exist
                resultHistory.add(response);
                dictionaryHistory.add(algo.getUpdatedDictionary());
            }

        }
        catch(InvalidWordException e){
            System.out.println("playGame:");
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
