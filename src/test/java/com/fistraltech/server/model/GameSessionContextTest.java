package com.fistraltech.server.model;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fistraltech.bot.selection.SelectionAlgo;
import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;
import com.fistraltech.core.WordEntropy;
import com.fistraltech.server.algo.AlgorithmRegistry;

@DisplayName("GameSessionContext Tests")
class GameSessionContextTest {

    @Test
    @DisplayName("suggestWord_usesCachedEntropyForUnfilteredEntropyStrategy")
    void suggestWord_usesCachedEntropyForUnfilteredEntropyStrategy() {
        Dictionary dictionary = dictionary("arose", "slate", "crane");
        AlgorithmRegistry registry = mock(AlgorithmRegistry.class);
        when(registry.normalizeId("ENTROPY")).thenReturn("ENTROPY");

        WordEntropy wordEntropy = mock(WordEntropy.class);
        when(wordEntropy.getMaximumEntropyWord(dictionary.getMasterSetOfWords())).thenReturn("arose");

        GameSessionContext context = new GameSessionContext(dictionary, registry);
        context.setSelectedStrategy("ENTROPY");
        context.setCachedWordEntropy(wordEntropy);

        assertEquals("arose", context.suggestWord());
        verify(wordEntropy).getMaximumEntropyWord(dictionary.getMasterSetOfWords());
    }

    @Test
    @DisplayName("suggestWord_usesSharedEntropyForFilteredEntropyStrategy")
    void suggestWord_usesSharedEntropyForFilteredEntropyStrategy() {
        Dictionary dictionary = dictionary("crane", "crate", "slate");
        AlgorithmRegistry registry = mock(AlgorithmRegistry.class);
        when(registry.normalizeId("ENTROPY")).thenReturn("ENTROPY");

        WordEntropy wordEntropy = mock(WordEntropy.class);
        when(wordEntropy.getMaximumEntropyWordLazy(any(Set.class), any(Set.class))).thenReturn("crate");

        GameSessionContext context = new GameSessionContext(dictionary, registry);
        context.setSelectedStrategy("ENTROPY");
        context.setCachedWordEntropy(wordEntropy);
        context.getWordFilter().removeAllOtherLetters('c', 0);

        assertEquals("crate", context.suggestWord());
        verify(wordEntropy).getMaximumEntropyWordLazy(any(Set.class), any(Set.class));
        verify(registry, times(0)).create(any(), any(Dictionary.class));
    }

    @Test
    @DisplayName("suggestWord_reusesCachedStatefulAlgorithm")
    void suggestWord_reusesCachedStatefulAlgorithm() {
        Dictionary dictionary = dictionary("arose", "slate", "crane");
        AlgorithmRegistry registry = mock(AlgorithmRegistry.class);
        when(registry.normalizeId("BELLMAN_FULL_DICTIONARY")).thenReturn("BELLMAN_FULL_DICTIONARY");
        when(registry.isStateful("BELLMAN_FULL_DICTIONARY")).thenReturn(true);

        SelectionAlgo algorithm = mock(SelectionAlgo.class);
        when(registry.create("BELLMAN_FULL_DICTIONARY", dictionary)).thenReturn(algorithm);
        when(algorithm.selectWord(any(Response.class), any(Dictionary.class))).thenReturn("slate", "crane");

        GameSessionContext context = new GameSessionContext(dictionary, registry);
        context.setSelectedStrategy("BELLMAN_FULL_DICTIONARY");

        assertEquals("slate", context.suggestWord());
        assertEquals("crane", context.suggestWord());
        verify(registry, times(1)).create("BELLMAN_FULL_DICTIONARY", dictionary);
    }

    @Test
    @DisplayName("filterStateDrivesRemainingWordCounts")
    void filterStateDrivesRemainingWordCounts() throws com.fistraltech.core.InvalidWordException {
        Dictionary dictionary = dictionary("arose", "slate", "crane");
        AlgorithmRegistry registry = mock(AlgorithmRegistry.class);
        GameSessionContext context = new GameSessionContext(dictionary, registry);

        assertEquals(3, context.getTotalWordsCount());
        assertEquals(3, context.getRemainingWordsCount());

        Response response = com.fistraltech.core.ResponseHelper.evaluate("arose", "crane");
        context.getWordFilter().update(response);

        assertNotNull(context.getFilteredDictionary());
        assertEquals(context.getFilteredDictionary().getWordCount(), context.getRemainingWordsCount());
    }

    @Test
    @DisplayName("suggestWord_returnsNullWhenFilterEliminatesAllCandidates")
    void suggestWord_returnsNullWhenFilterEliminatesAllCandidates() {
        Dictionary dictionary = dictionary("arose", "slate");
        AlgorithmRegistry registry = mock(AlgorithmRegistry.class);
        GameSessionContext context = new GameSessionContext(dictionary, registry);

        Response impossible = new Response("zzzzz");
        impossible.setStatus('z', 'G');
        impossible.setStatus('z', 'G');
        impossible.setStatus('z', 'G');
        impossible.setStatus('z', 'G');
        impossible.setStatus('z', 'G');
        context.getWordFilter().update(impossible);

        assertNull(context.suggestWord());
    }

    private static Dictionary dictionary(String... words) {
        Dictionary dictionary = new Dictionary(words[0].length());
        dictionary.addWords(Set.of(words));
        return dictionary;
    }
}