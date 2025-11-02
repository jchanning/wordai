package com.fistraltech.bot;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import com.fistraltech.core.Column;
import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;
import com.fistraltech.core.WordGame;

/**
 * Generates comprehensive analytics and CSV exports for word-guessing game sessions.
 * 
 * <p>This class collects, processes, and exports detailed game metrics to CSV files
 * for statistical analysis, algorithm comparison, and performance visualization.
 * 
 * <p><strong>Generated Output Files:</strong>
 * <ul>
 *   <li><strong>Summary CSV:</strong> Game outcomes (target word, attempts, win/loss)</li>
 *   <li><strong>Details CSV:</strong> Move-by-move breakdown with dictionary sizes</li>
 *   <li><strong>Columns CSV:</strong> Letter frequency analysis per position</li>
 * </ul>
 * 
 * <p><strong>Key Metrics Tracked:</strong>
 * <ul>
 *   <li>Target word and algorithm used</li>
 *   <li>Number of attempts to solve</li>
 *   <li>Dictionary size after each guess</li>
 *   <li>Win/loss status</li>
 *   <li>Column-wise letter distributions</li>
 * </ul>
 * 
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * // Setup
 * WordGame game = new WordGame(dictionary, dictionary, config);
 * WordGamePlayer player = new WordGamePlayer(game, strategy);
 * GameAnalytics analytics = new GameAnalytics(game, player);
 * 
 * // Play game
 * player.playGame(game);
 * 
 * // Export analytics
 * analytics.setIteration(1);
 * analytics.exportSummary("summary.csv");
 * analytics.exportDetails("details.csv");
 * analytics.exportColumns("columns.csv");
 * }</pre>
 * 
 * <p><strong>CSV Format Examples:</strong>
 * 
 * <p><em>Summary CSV:</em>
 * <pre>
 * Iteration,Algorithm,Target,Attempts,Status
 * 1,MaximumEntropy,AROSE,3,WON
 * 2,MaximumEntropy,STALE,4,WON
 * </pre>
 * 
 * <p><em>Details CSV:</em>
 * <pre>
 * Iteration,Attempt,Guess,Response,RemainingWords
 * 1,1,CRANE,RAARG,128
 * 1,2,STALE,AAGGG,5
 * 1,3,AROSE,GGGGG,1
 * </pre>
 * 
 * <p><strong>Use Cases:</strong>
 * <ul>
 *   <li>Compare different selection algorithms (entropy vs. frequency vs. random)</li>
 *   <li>Identify worst-case scenarios for specific strategies</li>
 *   <li>Visualize dictionary reduction patterns in Excel/Python</li>
 *   <li>Calculate average attempts across thousands of games</li>
 * </ul>
 * 
 * <p><strong>Thread Safety:</strong> This class is not thread-safe. Each game session
 * should have its own GameAnalytics instance.
 * 
 * @author Fistral Technologies
 * @see WordGamePlayer
 * @see DictionaryHistory
 * @see ResultHistory
 */
public class GameAnalytics {

    private final Player player;
    private final WordGame game;
    private final ResultHistory resultHistory;
    private final DictionaryHistory dictionaryHistory;
    private int iteration;

    public void setIteration(int iteration) {
        this.iteration = iteration;
    }

    public GameAnalytics(WordGame game,
                         Player player){
        this.game = game;
        this.player = player;
        this.resultHistory = player.getResultHistory();
        this.dictionaryHistory = player.getDictionaryHistory();
    }

    public void saveSummaryToFile(FileWriter summary) throws IOException{
        List<Response> responses = resultHistory.getHistory();
        StringBuilder gameSummary = new StringBuilder(iteration + "," + game.getTargetWord() + ","
                + player.getAlgo().getAlgoName() + "," + game.getNoOfAttempts() + ",");
        for(Response r: responses){
            gameSummary.append(r.getWord()).append(",");
        }
        summary.write(gameSummary + "\n");
        summary.flush();
    }

    public void saveDetailsToFile(FileWriter details) throws IOException{
        List<Response> responses = resultHistory.getHistory();
        List<Dictionary> dictionaries = dictionaryHistory.getHistory();
        for(int i = 0; i<dictionaries.size() && i< responses.size(); ++i){
            String detailedResult = iteration + "," + (i+1) + "," + game.getTargetWord() + "," + responses.get(i).getWord() + "," +
                    dictionaries.get(i).getWordCount() + "," + dictionaries.get(i).getLetterCount() + ","  + responses.get(i).getWinner();
            details.write(detailedResult + "\n");
        }
        details.flush();
    }

    public void saveColumnsToFile(FileWriter columns) throws IOException{
        List<Response> responses = resultHistory.getHistory();
        List<Dictionary> dictionaries = dictionaryHistory.getHistory();
        for(int i = 0; i<dictionaries.size() && i< responses.size(); ++i){
            String detailedResult = iteration + "," + (i+1) + "," + game.getTargetWord() + "," + responses.get(i).getWord();
            for(Column c: dictionaries.get(i).getColumns()){
                detailedResult += "," + c.length();
            }
                columns.write(detailedResult + "\n");
        }
        columns.flush();
    }
}
