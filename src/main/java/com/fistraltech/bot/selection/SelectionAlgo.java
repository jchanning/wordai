package com.fistraltech.bot.selection;

import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;

/**
 * Abstract base class for word selection algorithms.
 *
 * <p>Subclasses implement {@link #selectWord(Response, Dictionary)} and receive a
 * <em>pre-filtered</em> dictionary from the caller (either
 * {@link com.fistraltech.server.model.GameSession} or
 * {@link com.fistraltech.bot.WordGamePlayer}).  The base class is intentionally
 * stateless: it holds no filter, no dictionary copy, and no updated-dictionary
 * reference.  Callers are solely responsible for applying constraints and passing
 * the resulting candidate set.
 *
 * <p><strong>Stateful subclasses</strong> (e.g. {@link SelectBellmanFullDictionary})
 * may maintain their own per-game state (e.g. a set of already-guessed words) and
 * must implement {@link #reset()} to clear that state between games.  Stateless
 * subclasses inherit the no-op {@link #reset()}.
 *
 * <p><strong>Implementation guide:</strong>
 * <pre>{@code
 * public class SelectMyStrategy extends SelectionAlgo {
 *     public SelectMyStrategy(Dictionary dictionary) {
 *         super(dictionary);
 *         setAlgoName("MyStrategy");
 *     }
 *
 *     @Override
 *     public String selectWord(Response lastResponse, Dictionary filteredDict) {
 *         // filteredDict contains only valid candidates for the current game state
 *         return chosenWord;
 *     }
 * }
 * }</pre>
 *
 * <p><strong>Thread safety:</strong> not thread-safe; use one instance per game.
 *
 * @author Fistral Technologies
 * @see com.fistraltech.core.Filter
 * @see com.fistraltech.bot.WordGamePlayer
 * @see com.fistraltech.server.model.GameSession
 */
public abstract class SelectionAlgo {

    private String algoName;

    /**
     * No-op constructor retained for subclass compatibility.
     *
     * @param dictionary the game dictionary (unused by base class; subclasses may use it)
     */
    public SelectionAlgo(Dictionary dictionary) {
        // Base class holds no dictionary reference — callers manage filtering.
    }

    public String getAlgoName() {
        return algoName;
    }

    public void setAlgoName(String algoName) {
        this.algoName = algoName;
    }

    /**
     * Selects the next word to guess.
     *
     * @param lastResponse     the last response received (pass an empty {@link Response} for the
     *                         first guess)
     * @param filteredDictionary the current candidate dictionary, already filtered to only valid
     *                           words by the caller
     * @return the next word to guess
     */
    public abstract String selectWord(Response lastResponse, Dictionary filteredDictionary);

    /**
     * Resets algorithm state for a new game.
     *
     * <p>The default implementation is a no-op.  Stateful subclasses (e.g.
     * {@link SelectBellmanFullDictionary}) override this to clear per-game state.
     */
    public void reset() {
        // no-op for stateless algorithms
    }
}
