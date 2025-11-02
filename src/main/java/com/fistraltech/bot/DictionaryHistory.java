package com.fistraltech.bot;

import java.util.ArrayList;
import java.util.List;

import com.fistraltech.core.Dictionary;

/**
 * Tracks the evolution of the valid word dictionary as a game progresses.
 * 
 * <p>This class maintains a chronological sequence of {@link Dictionary} snapshots,
 * recording how the set of valid candidate words shrinks after each guess. This data
 * is essential for analyzing the effectiveness of word selection algorithms.
 * 
 * <p><strong>Purpose:</strong>
 * <ul>
 *   <li>Measure algorithm efficiency by tracking dictionary size reduction</li>
 *   <li>Calculate information gain per guess</li>
 *   <li>Identify optimal vs. suboptimal word choices</li>
 *   <li>Generate analytics for strategy comparison</li>
 * </ul>
 * 
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * DictionaryHistory history = new DictionaryHistory();
 * 
 * // Initial state (full dictionary)
 * history.add(dictionary.copy());  // 2315 words
 * 
 * // After first guess
 * dictionary = filter.applyFilter(dictionary);
 * history.add(dictionary.copy());  // 128 words remain
 * 
 * // After second guess
 * dictionary = filter.applyFilter(dictionary);
 * history.add(dictionary.copy());  // 5 words remain
 * 
 * // Analyze reduction
 * List<Integer> sizes = history.getDictionarySizes();
 * // [2315, 128, 5]
 * }</pre>
 * 
 * <p><strong>Performance Metrics:</strong><br>
 * The size progression reveals algorithm quality:
 * <ul>
 *   <li><strong>Good:</strong> Rapid, consistent reduction (e.g., 2315 → 200 → 20 → 2 → 1)</li>
 *   <li><strong>Poor:</strong> Slow reduction (e.g., 2315 → 1800 → 1200 → 800 → ...)</li>
 * </ul>
 * 
 * <p><strong>Integration:</strong><br>
 * Works in conjunction with:
 * <ul>
 *   <li>{@link ResultHistory} - Correlate dictionary sizes with specific guesses</li>
 *   <li>{@link GameAnalytics} - Export to CSV for detailed analysis</li>
 *   <li>{@link SelectionAlgo} - Compare different selection strategies</li>
 * </ul>
 * 
 * <p><strong>Thread Safety:</strong> This class is not thread-safe. External
 * synchronization is required if accessed concurrently.
 * 
 * @author Fistral Technologies
 * @see Dictionary
 * @see ResultHistory
 * @see GameAnalytics
 * @see SelectionAlgo
 */
public class DictionaryHistory {
    private final List<Dictionary> dictionaryList = new ArrayList<>();

    public List<Integer> getDictionarySizes() {
        List<Integer> sizes = new ArrayList<>();
        for (Dictionary d : dictionaryList) {
            sizes.add(d.getWordCount());
        }
        return sizes;
    }

    public List<Dictionary> getHistory(){
        return  dictionaryList;
    }

    public void add(Dictionary dictionary){
        dictionaryList.add(dictionary);
    }
}
