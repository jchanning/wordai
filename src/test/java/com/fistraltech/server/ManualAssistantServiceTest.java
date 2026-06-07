package com.fistraltech.server;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fistraltech.core.Dictionary;
import com.fistraltech.core.InvalidWordException;
import com.fistraltech.core.Response;
import com.fistraltech.server.algo.AlgorithmRegistry;
import com.fistraltech.server.model.ManualAssistantSession;
import com.fistraltech.web.ApiResourceNotFoundException;

@DisplayName("ManualAssistantService Tests")
class ManualAssistantServiceTest {

    @Test
    @DisplayName("createSession_usesDictionaryAndStrategy")
    void createSession_usesDictionaryAndStrategy() throws Exception {
        DictionaryService dictionaryService = mock(DictionaryService.class);
        AlgorithmFeatureService featureService = mock(AlgorithmFeatureService.class);
        AlgorithmRegistry registry = AlgorithmRegistry.withDefaults();

        Dictionary dictionary = new Dictionary(5);
        dictionary.addWords(Set.of("arose", "crane", "stare", "slate", "raise"));

        when(dictionaryService.getMasterDictionary("default")).thenReturn(dictionary);
        when(featureService.isAlgorithmEnabled("RANDOM")).thenReturn(true);

        ManualAssistantService service = new ManualAssistantService(dictionaryService, registry, featureService);
        ManualAssistantSession session = service.createSession(null, null);

        assertNotNull(session.getSessionId());
        assertEquals("default", session.getDictionaryId());
        assertEquals("RANDOM", session.getSelectedStrategy());
        assertEquals(5, session.getRemainingWordsCount());
    }

    @Test
    @DisplayName("submitFeedback_updatesSessionAndAddsRemainingCount")
    void submitFeedback_updatesSessionAndAddsRemainingCount() throws Exception {
        DictionaryService dictionaryService = mock(DictionaryService.class);
        AlgorithmFeatureService featureService = mock(AlgorithmFeatureService.class);
        AlgorithmRegistry registry = AlgorithmRegistry.withDefaults();

        Dictionary dictionary = new Dictionary(5);
        dictionary.addWords(Set.of("arose", "stare", "crane"));

        when(dictionaryService.getMasterDictionary("default")).thenReturn(dictionary);
        when(featureService.isAlgorithmEnabled("RANDOM")).thenReturn(true);

        ManualAssistantService service = new ManualAssistantService(dictionaryService, registry, featureService);
        ManualAssistantSession session = service.createSession("default", "RANDOM");

        Response response = service.submitFeedback(session.getSessionId(), "arose", "GGGGG");
        assertEquals("GGGGG", response.toString());
        assertEquals(1, session.getAttemptCount());
        assertEquals(1, session.getRemainingWordsCount());

        String suggestion = service.suggestWord(session.getSessionId());
        assertEquals("arose", suggestion);
    }

    @Test
    @DisplayName("submitFeedback_rejectsWrongLengthWord")
    void submitFeedback_rejectsWrongLengthWord() throws Exception {
        DictionaryService dictionaryService = mock(DictionaryService.class);
        AlgorithmFeatureService featureService = mock(AlgorithmFeatureService.class);
        AlgorithmRegistry registry = AlgorithmRegistry.withDefaults();

        Dictionary dictionary = new Dictionary(5);
        dictionary.addWords(Set.of("arose", "crane"));

        when(dictionaryService.getMasterDictionary("default")).thenReturn(dictionary);
        when(featureService.isAlgorithmEnabled("RANDOM")).thenReturn(true);

        ManualAssistantService service = new ManualAssistantService(dictionaryService, registry, featureService);
        ManualAssistantSession session = service.createSession("default", "RANDOM");

        assertThrows(InvalidWordException.class,
            () -> service.submitFeedback(session.getSessionId(), "cat", "GGG"));
    }

    @Test
    @DisplayName("submitFeedback_rejectsBlankGuess")
    void submitFeedback_rejectsBlankGuess() throws Exception {
        DictionaryService dictionaryService = mock(DictionaryService.class);
        AlgorithmFeatureService featureService = mock(AlgorithmFeatureService.class);
        AlgorithmRegistry registry = AlgorithmRegistry.withDefaults();

        Dictionary dictionary = new Dictionary(5);
        dictionary.addWords(Set.of("arose", "crane"));

        when(dictionaryService.getMasterDictionary("default")).thenReturn(dictionary);
        when(featureService.isAlgorithmEnabled("RANDOM")).thenReturn(true);

        ManualAssistantService service = new ManualAssistantService(dictionaryService, registry, featureService);
        ManualAssistantSession session = service.createSession("default", "RANDOM");

        assertThrows(InvalidWordException.class,
            () -> service.submitFeedback(session.getSessionId(), "   ", "GGGGG"));
    }

    @Test
    @DisplayName("getSession_throwsWhenMissing")
    void getSession_throwsWhenMissing() {
        DictionaryService dictionaryService = mock(DictionaryService.class);
        AlgorithmFeatureService featureService = mock(AlgorithmFeatureService.class);
        AlgorithmRegistry registry = AlgorithmRegistry.withDefaults();
        ManualAssistantService service = new ManualAssistantService(dictionaryService, registry, featureService);

        assertThrows(ApiResourceNotFoundException.class, () -> service.getSession("missing"));
    }

    @Test
    @DisplayName("setStrategy_rejectsDisabledAlgorithm")
    void setStrategy_rejectsDisabledAlgorithm() throws Exception {
        DictionaryService dictionaryService = mock(DictionaryService.class);
        AlgorithmFeatureService featureService = mock(AlgorithmFeatureService.class);
        AlgorithmRegistry registry = AlgorithmRegistry.withDefaults();

        Dictionary dictionary = new Dictionary(5);
        dictionary.addWords(Set.of("arose", "crane"));

        when(dictionaryService.getMasterDictionary("default")).thenReturn(dictionary);
        when(featureService.isAlgorithmEnabled("RANDOM")).thenReturn(true);
        when(featureService.isAlgorithmEnabled("ENTROPY")).thenReturn(false);

        ManualAssistantService service = new ManualAssistantService(dictionaryService, registry, featureService);
        ManualAssistantSession session = service.createSession("default", "RANDOM");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
            () -> service.setStrategy(session.getSessionId(), "ENTROPY"));
        assertTrue(error.getMessage().contains("not enabled"));
    }

    @Test
    @DisplayName("setStrategy_updatesSessionStrategy")
    void setStrategy_updatesSessionStrategy() throws Exception {
        DictionaryService dictionaryService = mock(DictionaryService.class);
        AlgorithmFeatureService featureService = mock(AlgorithmFeatureService.class);
        AlgorithmRegistry registry = AlgorithmRegistry.withDefaults();

        Dictionary dictionary = new Dictionary(5);
        dictionary.addWords(Set.of("arose", "crane", "stare"));

        when(dictionaryService.getMasterDictionary("default")).thenReturn(dictionary);
        when(featureService.isAlgorithmEnabled("RANDOM")).thenReturn(true);
        when(featureService.isAlgorithmEnabled("ENTROPY")).thenReturn(true);

        ManualAssistantService service = new ManualAssistantService(dictionaryService, registry, featureService);
        ManualAssistantSession session = service.createSession("default", "RANDOM");

        String selected = service.setStrategy(session.getSessionId(), "ENTROPY");

        assertEquals("ENTROPY", selected);
        assertEquals("ENTROPY", session.getSelectedStrategy());
    }
}