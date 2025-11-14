    package com.fistraltech.analysis;

import java.io.FileWriter;
import java.io.IOException;

import com.fistraltech.bot.DictionaryHistory;
import com.fistraltech.bot.GameAnalytics;
import com.fistraltech.bot.Player;
import com.fistraltech.bot.ResultHistory;
import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;
import com.fistraltech.core.WordGame;

/** Used to collect information and analyse the performance of a Player's selection algorithm.  The analyser plays
 * against every word in the Dictionary.  Multiple iterations of playing against every single word produces a stable
 * result of the performance.*/

public class PlayerAnalyser {
    private final Player player;

    private static final String resultFolder = "D:\\OneDrive\\Projects\\Wordlex\\Results\\";

    public PlayerAnalyser( Player player){
        this.player = player;
    }

    /** Plays one full cycle using every word in the dictionary*/
    public void analyseGamePlay() throws Exception {
        WordGame wg = player.getWordGame();
        Dictionary allWords = player.getDictionary();

        String summaryFile = resultFolder + "summary-" + System.currentTimeMillis();
        String detailFile = resultFolder + "detail-" + System.currentTimeMillis();

        FileWriter summary = new FileWriter(summaryFile);
        FileWriter details = new FileWriter(detailFile);

        for(String word: allWords.getMasterSetOfWords()){
            try {
                wg.setTargetWord(word);
                player.playGame(wg);
            }
            catch(Exception ex){
                ex.printStackTrace();
            }

            try {
                GameAnalytics ga = new GameAnalytics(wg, player);
                ga.saveSummaryToFile(summary);
                ga.saveDetailsToFile(details);
            }
            catch(IOException exception){
            }
        }
    }


    private static void printResultHistory(String fileName, ResultHistory rh) throws IOException {
        FileWriter f = new FileWriter(fileName);
        for( Response r: rh.getHistory()){
            f.write(r.toString());
        }
    }

    private static void printDictionaryHistory(String fileName, DictionaryHistory dh) throws IOException {
        try (FileWriter f = new FileWriter(fileName)) {
			int counter = 0;
			for( Dictionary d: dh.getHistory()){
			    f.write(counter++ + "," + d.getMasterSetOfWords().size() + "," + d.getLetterCount());
			}
		}
    }
}
