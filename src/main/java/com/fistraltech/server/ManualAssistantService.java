package com.fistraltech.server;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.fistraltech.core.Dictionary;
import com.fistraltech.core.InvalidWordException;
import com.fistraltech.core.InvalidWordLengthException;
import com.fistraltech.core.ManualWordleFeedbackAdapter;
import com.fistraltech.core.Response;
import com.fistraltech.server.algo.AlgorithmRegistry;
import com.fistraltech.server.model.ManualAssistantSession;
import com.fistraltech.web.ApiResourceNotFoundException;

/**
 * Service for target-free manual Wordle assistant sessions.
 */
@Service
public class ManualAssistantService {

    private static final Logger logger = Logger.getLogger(ManualAssistantService.class.getName());

    private final DictionaryService dictionaryService;
    private final AlgorithmRegistry algorithmRegistry;
    private final AlgorithmFeatureService algorithmFeatureService;
    private final ConcurrentMap<String, ManualAssistantSession> sessions = new ConcurrentHashMap<>();

    public ManualAssistantService(DictionaryService dictionaryService,
                                  AlgorithmRegistry algorithmRegistry,
                                  AlgorithmFeatureService algorithmFeatureService) {
        this.dictionaryService = dictionaryService;
        this.algorithmRegistry = algorithmRegistry;
        this.algorithmFeatureService = algorithmFeatureService;
    }

    public ManualAssistantSession createSession(String dictionaryId, String strategy) throws InvalidWordException {
        String effectiveDictionaryId = (dictionaryId == null || dictionaryId.isBlank()) ? "default" : dictionaryId;
        String normalizedStrategy = algorithmRegistry.normalizeId(strategy);

        if (!algorithmFeatureService.isAlgorithmEnabled(normalizedStrategy)) {
            throw new IllegalArgumentException("Algorithm '" + normalizedStrategy + "' is not enabled");
        }

        Dictionary dictionary = dictionaryService.getMasterDictionary(effectiveDictionaryId);
        if (dictionary == null) {
            throw new InvalidWordException("Dictionary not found: " + effectiveDictionaryId);
        }

        String sessionId = UUID.randomUUID().toString();
        ManualAssistantSession session = new ManualAssistantSession(
            sessionId,
            effectiveDictionaryId,
            dictionary,
            algorithmRegistry,
            normalizedStrategy);

        sessions.put(sessionId, session);
        logger.info(() -> "Created assistant session " + sessionId + " (dictionary=" + effectiveDictionaryId
            + ", strategy=" + normalizedStrategy + ")");
        return session;
    }

    public ManualAssistantSession getSession(String sessionId) {
        ManualAssistantSession session = sessions.get(sessionId);
        if (session == null) {
            throw new ApiResourceNotFoundException("Assistant session not found",
                "Assistant session " + sessionId + " does not exist");
        }
        return session;
    }

    public void deleteSession(String sessionId) {
        sessions.remove(sessionId);
    }

    public Response submitFeedback(String sessionId, String guessedWord, String feedbackPattern)
        throws InvalidWordException {
        ManualAssistantSession session = getSession(sessionId);
        if (guessedWord == null || guessedWord.isBlank()) {
            throw new InvalidWordException("Guessed word is required");
        }

        String normalizedGuess = guessedWord.trim().toLowerCase();
        if (normalizedGuess.length() != session.getWordLength()) {
            throw new InvalidWordLengthException("Invalid word, it is the wrong length");
        }

        Response response;
        try {
            response = ManualWordleFeedbackAdapter.fromWordleFeedback(normalizedGuess, feedbackPattern);
        } catch (IllegalArgumentException e) {
            throw new InvalidWordException(e.getMessage());
        }

        synchronized (session) {
            session.applyFeedback(response);
            response.setRemainingWordsCount(session.getRemainingWordsCount());
        }

        return response;
    }

    public String suggestWord(String sessionId) {
        ManualAssistantSession session = getSession(sessionId);
        synchronized (session) {
            return session.suggestWord();
        }
    }

    public String setStrategy(String sessionId, String strategy) {
        ManualAssistantSession session = getSession(sessionId);
        String normalizedStrategy = algorithmRegistry.normalizeId(strategy);

        if (!algorithmFeatureService.isAlgorithmEnabled(normalizedStrategy)) {
            throw new IllegalArgumentException("Algorithm '" + normalizedStrategy + "' is not enabled");
        }

        synchronized (session) {
            session.setSelectedStrategy(normalizedStrategy);
            return session.getSelectedStrategy();
        }
    }
}