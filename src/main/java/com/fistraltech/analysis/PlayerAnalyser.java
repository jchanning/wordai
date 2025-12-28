    package com.fistraltech.analysis;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.fistraltech.bot.DictionaryHistory;
import com.fistraltech.bot.GameAnalytics;
import com.fistraltech.bot.Player;
import com.fistraltech.bot.ResultHistory;
import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;
import com.fistraltech.core.WordGame;
import com.fistraltech.server.dto.AnalysisGameResult;
import com.fistraltech.server.dto.AnalysisResponse;
import com.fistraltech.util.ConfigManager;

/** 
 * Used to collect information and analyse the performance of a Player's selection algorithm.  
 * The analyser plays against every word in the Dictionary. Multiple iterations of playing 
 * against every single word produces a stable result of the performance.
 * 
 * <p>This class now supports both file-based output (legacy) and in-memory result objects
 * for integration with the web UI.
 */
public class PlayerAnalyser {
    private final Player player;
    private final boolean enableFileOutput;
    private final String outputBasePath;
    
    /**
     * Creates a new PlayerAnalyser with file output configuration from properties.
     * 
     * @param player The player whose algorithm will be analyzed
     */
    public PlayerAnalyser(Player player){
        this.player = player;
        ConfigManager config = ConfigManager.getInstance();
        this.enableFileOutput = config.getProperty("analysis.output.enabled", "false").equalsIgnoreCase("true");
        this.outputBasePath = config.getProperty("analysis.output.path", 
            config.getProperty("output.base.path", System.getProperty("user.home") + "/wordai-analysis"));
    }
    
    /**
     * Creates a new PlayerAnalyser with explicit file output configuration.
     * 
     * @param player The player whose algorithm will be analyzed
     * @param enableFileOutput Whether to write results to CSV files
     * @param outputBasePath Base path for output files (used if enableFileOutput is true)
     */
    public PlayerAnalyser(Player player, boolean enableFileOutput, String outputBasePath){
        this.player = player;
        this.enableFileOutput = enableFileOutput;
        this.outputBasePath = outputBasePath;
    }

    /**
     * Plays one full cycle using every word in the dictionary and returns results.
     * Optionally writes results to CSV files based on configuration.
     * 
     * @return AnalysisResponse containing complete analysis results
     * @throws Exception if game play fails
     */
    public AnalysisResponse analyseGamePlay() throws Exception {
        return analyseGamePlay(null);
    }
    
    /**
     * Plays games up to maxGames limit and returns results.
     * 
     * @param maxGames Maximum number of games to play (null for all words)
     * @return AnalysisResponse containing analysis results
     * @throws Exception if game play fails
     */
    public AnalysisResponse analyseGamePlay(Integer maxGames) throws Exception {
        WordGame wg = player.getWordGame();
        Dictionary allWords = player.getDictionary();
        
        AnalysisResponse response = new AnalysisResponse();
        response.setAlgorithm(player.getAlgo().getAlgoName());
        response.setDictionaryId("custom"); // Could be enhanced to track actual dictionary ID
        
        List<AnalysisGameResult> gameResults = new ArrayList<>();
        List<Integer> wonAttempts = new ArrayList<>();
        int totalGames = 0;
        int gamesWon = 0;
        int gamesLost = 0;
        
        FileWriter summary = null;
        FileWriter details = null;
        
        try {
            // Setup file writers if enabled
            if (enableFileOutput) {
                ensureOutputDirectoryExists();
                long timestamp = System.currentTimeMillis();
                summary = new FileWriter(outputBasePath + "/summary-" + timestamp + ".csv");
                details = new FileWriter(outputBasePath + "/detail-" + timestamp + ".csv");
                
                // Write headers
                summary.write("Iteration,TargetWord,Algorithm,Attempts,Guesses\n");
                details.write("Iteration,Attempt,TargetWord,Guess,RemainingWords,LetterCount,Winner\n");
            }
            
            // Play games
            List<String> words = new ArrayList<>(allWords.getMasterSetOfWords());
            int gamesToPlay = (maxGames != null && maxGames > 0) ? Math.min(maxGames, words.size()) : words.size();
            
            System.out.println("Starting analysis of " + gamesToPlay + " games...");
            
            for(int i = 0; i < gamesToPlay; i++){
                String word = words.get(i);
                totalGames++;
                
                // Reset player state before each game
                player.getResultHistory().getHistory().clear();
                player.getDictionaryHistory().getHistory().clear();
                player.getAlgo().reset();
                
                try {
                    wg.setTargetWord(word);
                    player.playGame(wg);
                }
                catch(Exception ex){
                    // Game failed (likely max attempts reached) - continue to collect result
                }
                
                // Collect game result (works for both won and lost games)
                AnalysisGameResult gameResult = collectGameResult(wg, player, i + 1);
                gameResults.add(gameResult);
                
                if (gameResult.isWon()) {
                    gamesWon++;
                    wonAttempts.add(gameResult.getAttempts());
                } else {
                    gamesLost++;
                }
                
                // Progress logging (every 100 games or at key milestones)
                if ((i + 1) % 100 == 0 || (i + 1) == gamesToPlay || (i + 1) == 10) {
                    double currentWinRate = totalGames > 0 ? (gamesWon * 100.0 / totalGames) : 0;
                    System.out.println(String.format("Progress: %d/%d games (%.1f%% win rate)", 
                        i + 1, gamesToPlay, currentWinRate));
                }
                
                // Write to files if enabled
                if (enableFileOutput && summary != null && details != null) {
                    try {
                        GameAnalytics ga = new GameAnalytics(wg, player);
                        ga.setIteration(i + 1);
                        ga.saveSummaryToFile(summary);
                        ga.saveDetailsToFile(details);
                    } catch(Exception ex) {
                        // Log but don't fail the analysis
                        System.err.println("Failed to write analytics for iteration " + (i + 1) + ": " + ex.getMessage());
                    }
                }
            }
            
            // Calculate statistics
            response.setTotalGames(totalGames);
            response.setGamesWon(gamesWon);
            response.setGamesLost(gamesLost);
            response.setWinRate(totalGames > 0 ? (gamesWon * 100.0 / totalGames) : 0);
            
            if (!wonAttempts.isEmpty()) {
                response.setMinAttempts(wonAttempts.stream().min(Integer::compareTo).orElse(null));
                response.setMaxAttempts(wonAttempts.stream().max(Integer::compareTo).orElse(null));
                response.setAvgAttempts(wonAttempts.stream().mapToInt(Integer::intValue).average().orElse(0));
            }
            
            response.setGameResults(gameResults);
            response.setCompleted(true);
            response.setMessage("Analysis completed successfully for " + totalGames + " games");
            
        } finally {
            if (summary != null) {
                try { summary.close(); } catch (IOException e) { /* ignore */ }
            }
            if (details != null) {
                try { details.close(); } catch (IOException e) { /* ignore */ }
            }
        }
        
        return response;
    }
    
    /**
     * Collects detailed game result information.
     */
    private AnalysisGameResult collectGameResult(WordGame wg, Player player, int iteration) {
        ResultHistory rh = player.getResultHistory();
        DictionaryHistory dh = player.getDictionaryHistory();
        
        List<Response> responses = rh.getHistory();
        List<Dictionary> dictionaries = dh.getHistory();
        
        List<AnalysisGameResult.AnalysisGuess> guesses = new ArrayList<>();
        
        for(int i = 0; i < dictionaries.size() && i < responses.size(); i++){
            AnalysisGameResult.AnalysisGuess guess = new AnalysisGameResult.AnalysisGuess();
            guess.setAttemptNumber(i + 1);
            guess.setGuess(responses.get(i).getWord());
            guess.setResponse(responses.get(i).toString());
            guess.setRemainingWords(dictionaries.get(i).getWordCount());
            guess.setLetterCount(dictionaries.get(i).getLetterCount());
            guesses.add(guess);
        }
        
        boolean won = !responses.isEmpty() && responses.get(responses.size() - 1).getWinner();
        
        AnalysisGameResult result = new AnalysisGameResult();
        result.setTargetWord(wg.getTargetWord());
        result.setAttempts(wg.getNoOfAttempts());
        result.setWon(won);
        result.setAlgorithm(player.getAlgo().getAlgoName());
        result.setGuesses(guesses);
        
        return result;
    }
    
    /**
     * Ensures the output directory exists.
     */
    private void ensureOutputDirectoryExists() throws IOException {
        Path outputPath = Paths.get(outputBasePath);
        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
        }
    }

    // Legacy methods for backward compatibility
    
    private static void printResultHistory(String fileName, ResultHistory rh) throws IOException {
        try (FileWriter f = new FileWriter(fileName)) {
            for( Response r: rh.getHistory()){
                f.write(r.toString());
            }
        }
    }

    private static void printDictionaryHistory(String fileName, DictionaryHistory dh) throws IOException {
        try (FileWriter f = new FileWriter(fileName)) {
            int counter = 0;
            for( Dictionary d: dh.getHistory()){
                f.write(counter++ + "," + d.getMasterSetOfWords().size() + "," + d.getLetterCount() + "\n");
            }
        }
    }
}
