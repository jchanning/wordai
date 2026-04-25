package com.fistraltech.server;

import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fistraltech.core.WordGame;
import com.fistraltech.server.model.GameSession;
import com.fistraltech.server.repository.ActiveGameSessionRepository;

@DisplayName("SessionPersistenceService Tests")
class SessionPersistenceServiceTest {

    @Test
    @DisplayName("save_repositoryFailure_throwsIllegalStateException")
    void save_repositoryFailure_throwsIllegalStateException() {
        ActiveGameSessionRepository repository = mock(ActiveGameSessionRepository.class);
        SessionPersistenceService service = new SessionPersistenceService(repository);

        GameSession session = mock(GameSession.class);
        WordGame wordGame = mock(WordGame.class);
        when(session.getGameId()).thenReturn("game-1");
        when(session.getDictionaryId()).thenReturn("default");
        when(session.getBrowserSessionId()).thenReturn("browser-1");
        when(session.getWordGame()).thenReturn(wordGame);
        when(wordGame.getTargetWord()).thenReturn("arose");
        when(session.getSelectedStrategy()).thenReturn("RANDOM");

        doThrow(new RuntimeException("db failure")).when(repository).save(any());

        assertThrows(IllegalStateException.class, () -> service.save(session, 1L));
    }
}