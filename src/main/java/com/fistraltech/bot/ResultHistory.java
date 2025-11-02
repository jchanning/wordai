package com.fistraltech.bot;

import java.util.ArrayList;
import java.util.List;

import com.fistraltech.core.Response;

/**
 * Tracks the sequence of guess responses during a word-guessing game session.
 * 
 * <p>This class maintains an ordered history of all {@link Response} objects generated
 * during a game, allowing for post-game analysis, performance metrics, and debugging.
 * 
 * <p><strong>Purpose:</strong>
 * <ul>
 *   <li>Record each guess and its feedback for analysis</li>
 *   <li>Calculate attempt counts and success rates</li>
 *   <li>Export game data for CSV analytics</li>
 *   <li>Debug bot strategy effectiveness</li>
 * </ul>
 * 
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * ResultHistory history = new ResultHistory();
 * 
 * // During game play
 * Response response1 = game.guess("CRANE");
 * history.add(response1);
 * 
 * Response response2 = game.guess("STALE");
 * history.add(response2);
 * 
 * // After game
 * int attempts = history.getHistory().size();
 * System.out.println("Solved in " + attempts + " guesses");
 * }</pre>
 * 
 * <p><strong>Integration:</strong><br>
 * This class is typically used in conjunction with:
 * <ul>
 *   <li>{@link DictionaryHistory} - Track dictionary size reduction</li>
 *   <li>{@link GameAnalytics} - Generate CSV reports and statistics</li>
 *   <li>{@link WordGamePlayer} - Automated bot gameplay</li>
 * </ul>
 * 
 * <p><strong>Thread Safety:</strong> This class is not thread-safe. External
 * synchronization is required if accessed concurrently.
 * 
 * @author Fistral Technologies
 * @see Response
 * @see DictionaryHistory
 * @see GameAnalytics
 */
public class ResultHistory {
    private final List<Response> history = new ArrayList<>();

    public void add(Response response){
        history.add(response);
    }

    public List<Response> getHistory() {
        return history;
    }
}
