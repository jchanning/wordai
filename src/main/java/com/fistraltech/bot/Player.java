package com.fistraltech.bot;

import com.fistraltech.bot.selection.SelectionAlgo;
import com.fistraltech.core.Dictionary;
import com.fistraltech.core.DictionaryHistory;
import com.fistraltech.core.ResultHistory;
import com.fistraltech.core.WordGame;

/** Interface defining a player of games */
public interface Player {

    void playGame(WordGame wg);

    DictionaryHistory getDictionaryHistory();
    ResultHistory getResultHistory();

    Dictionary getDictionary();

    SelectionAlgo getAlgo();

    WordGame getWordGame();
}
