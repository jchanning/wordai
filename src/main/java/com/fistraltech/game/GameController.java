package com.fistraltech.game;

import java.util.Set;

import com.fistraltech.core.Dictionary;
import com.fistraltech.core.InvalidWordException;
import com.fistraltech.core.Response;
import com.fistraltech.core.WordGame;
import com.fistraltech.util.ConfigManager;

/**
 * Controller class that manages the game flow and coordinates between the word dictionaries
 * and the core WordGame engine.
 * 
 * <p>This class acts as a facade for the Wordle-like game, handling dictionary initialization,
 * target word validation, and guess processing. It maintains two separate dictionaries:
 * <ul>
 *   <li>All valid words - used for validating player guesses</li>
 *   <li>Game words - the subset of words that can be selected as target words</li>
 * </ul>
 * 
 * <p><strong>Typical usage:</strong>
 * <pre>{@code
 * GameController controller = new GameController();
 * controller.loadAllValidWords(validWordSet);
 * controller.loadGameWords(5, targetWordSet);
 * controller.setTargetWord("AROSE");
 * Response response = controller.play("RAISE");
 * }</pre>
 * 
 * @author Fistral Technologies
 */

public class GameController {
    
    /** Dictionary containing all valid words that players can guess */
    private Dictionary allValidWords;
    
    /** Dictionary containing the subset of words that can be used as target words */
    private Dictionary gameWords;
    
    /** The length of words used in this game instance */
    private int wordLength;
    
    /** The underlying WordGame engine that processes guesses and manages game state */
    private WordGame game;
    
    /** Configuration object containing game settings and parameters */
    private ConfigManager configManager;
    private com.fistraltech.util.Config config;

    /**
     * Retrieves the dictionary containing all valid words that can be guessed in the game.
     * 
     * <p>This dictionary is used to validate player guesses. It typically contains a larger
     * set of words than the game words dictionary, which only includes words that can be
     * selected as target words.
     * 
     * @return the Dictionary containing all valid guessable words, or {@code null} if not yet initialized
     * @see #loadAllValidWords(Set)
     */
    public Dictionary getAllValidWords() {
        return allValidWords;
    }

    /**
     * Constructs a new GameController and initializes the underlying WordGame engine.
     * 
     * <p><strong>Note:</strong> This constructor initializes the game with {@code null} dictionaries
     * and config. You must call {@link #loadAllValidWords(Set)} and {@link #loadGameWords(int, Set)}
     * before the controller can be used.
     * 
     * <p><strong>Warning:</strong> Calling {@link #setTargetWord(String)} or {@link #play(String)}
     * before loading dictionaries will result in exceptions.
     */
    public GameController(){
        this.configManager = ConfigManager.getInstance();
        this.config = configManager.createGameConfig();
        this.config.setMaxAttempts(6);
        this.game = new WordGame(allValidWords, allValidWords, config);
    }

    /**
     * Sets the target word that players must guess in the current game.
     * 
     * <p>The target word must exist in the all valid words dictionary. Once set,
     * players can make guesses against this word using the {@link #play(String)} method.
     * 
     * @param word the target word to be guessed (case-insensitive, will be converted to uppercase)
     * @throws InvalidWordException if the dictionary hasn't been initialized ({@code allValidWords} is {@code null})
     * @throws InvalidWordException if the specified word is not in the valid words dictionary
     * @see #play(String)
     * @see #loadAllValidWords(Set)
     */
    public void setTargetWord(String word) throws InvalidWordException{
        if(allValidWords == null){
            throw new InvalidWordException("Dictionary is empty load words");
        }
        if(!allValidWords.contains(word)){
            throw new InvalidWordException("Word is not in dictionary");
        }

        game.setTargetWord(word);
    }

    /**
     * Initializes the dictionary of all valid words that players can guess.
     * 
     * <p>This dictionary is typically larger than the game words dictionary and includes
     * all words that the game will accept as valid guesses. The word length for this
     * dictionary is determined by the current {@code wordLength} field.
     * 
     * <p><strong>Important:</strong> This method must be called before {@link #setTargetWord(String)}
     * or {@link #play(String)} to ensure proper validation of guesses.
     * 
     * @param words a Set of String words to populate the dictionary (duplicates are automatically handled)
     * @see #loadGameWords(int, Set)
     * @see Dictionary
     */
    public void loadAllValidWords(Set<String> words){
        this.allValidWords = new Dictionary(wordLength);
        this.allValidWords.addWords(words);
        reinitializeGame();
    }

    /**
     * Initializes the dictionary of words that can be selected as target words in the game.
     * 
     * <p>This dictionary typically contains a curated subset of common or appropriate words
     * that provide good gameplay. For example, a Wordle-like game might exclude obscure
     * or offensive words from the target pool while still allowing them as valid guesses.
     * 
     * <p>This method also sets the word length for the game, which affects both dictionaries.
     * 
     * @param wordLength the required length of all words in the game (typically 5 for Wordle-style games)
     * @param words a Set of String words that can be selected as target words
     * @see #loadAllValidWords(Set)
     * @see Dictionary
     */
    public void loadGameWords(int wordLength, Set<String> words){
        this.wordLength = wordLength;
        this.gameWords = new Dictionary(wordLength);
        this.gameWords.addWords(words);
        reinitializeGame();
    }
    
    /**
     * Reinitializes the WordGame instance with the current dictionaries and config.
     * Called automatically after loading dictionaries to ensure the game has access
     * to the updated dictionary references.
     */
    private void reinitializeGame() {
        this.game = new WordGame(allValidWords, gameWords, config);
    }

    /**
     * Processes a player's guess and returns the result indicating which letters are correct.
     * 
     * <p>This is the main game interaction method. It submits a guess to the underlying
     * WordGame engine and returns a Response object that indicates:
     * <ul>
     *   <li>Which letters are in the correct position (Green/Correct)</li>
     *   <li>Which letters are in the word but wrong position (Amber/Present)</li>
     *   <li>Which letters are not in the word at all (Red/Absent)</li>
     * </ul>
     * 
     * <p><strong>Example response interpretation:</strong>
     * For target word "AROSE" and guess "RAISE":
     * <ul>
     *   <li>R - Amber (in word, wrong position)</li>
     *   <li>A - Amber (in word, wrong position)</li>
     *   <li>I - Red (not in word)</li>
     *   <li>S - Green (correct position)</li>
     *   <li>E - Green (correct position)</li>
     * </ul>
     * 
     * @param word the player's guessed word (must be in the valid words dictionary)
     * @return a Response object containing the result for each letter in the guess
     * @throws InvalidWordException if the word length doesn't match the game's word length
     * @throws InvalidWordException if the word is not in the dictionary of valid words
     * @see Response
     * @see WordGame#guess(String)
     */
    public Response play(String word) throws InvalidWordException {
        Response r = game.guess(word);
        return r;
    }
}
