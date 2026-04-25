package com.fistraltech.server;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;
import com.fistraltech.core.WordGame;
import com.fistraltech.server.dto.GameHistoryDto;
import com.fistraltech.server.model.GameSession;
import com.fistraltech.server.model.PersistedGame;
import com.fistraltech.server.repository.PlayerGameRepository;

@DisplayName("PlayerGameService Tests")
class PlayerGameServiceTest {

    @Test
    @DisplayName("saveGame_persistsCompletedGameWithEncodedHistory")
    void saveGame_persistsCompletedGameWithEncodedHistory() {
        PlayerGameRepository repository = mock(PlayerGameRepository.class);
        PlayerGameService service = new PlayerGameService(repository);

        GameSession session = mock(GameSession.class);
        WordGame wordGame = mock(WordGame.class);
        Dictionary dictionary = mock(Dictionary.class);
        when(session.getWordGame()).thenReturn(wordGame);
        when(session.getGameId()).thenReturn("game-1");
        when(session.getCurrentAttempts()).thenReturn(2);
        when(session.getMaxAttempts()).thenReturn(6);
        when(wordGame.getTargetWord()).thenReturn("arose");
        when(wordGame.getDictionary()).thenReturn(dictionary);
        when(dictionary.getWordLength()).thenReturn(5);
        when(wordGame.getGuesses()).thenReturn(List.of(response("crane", "RARRA", false), response("arose", "GGGGG", true)));

        service.saveGame(42L, null, session, null);

        ArgumentCaptor<PersistedGame> captor = ArgumentCaptor.forClass(PersistedGame.class);
        verify(repository).save(captor.capture());

        PersistedGame saved = captor.getValue();
        assertEquals(42L, saved.getUserId());
        assertNull(saved.getClientIpAddress());
        assertEquals("game-1", saved.getGameId());
        assertEquals("arose", saved.getTargetWord());
        assertEquals(5, saved.getWordLength());
        assertEquals("default", saved.getDictionaryId());
        assertEquals("crane,arose", saved.getGuessWords());
        assertEquals("RARRA;GGGGG", saved.getGuessResponses());
        assertEquals("WON", saved.getResult());
        assertEquals(2, saved.getAttemptsUsed());
        assertEquals(6, saved.getMaxAttempts());
        assertTrue(saved.getCompletedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    @DisplayName("saveGame_repositoryFailureDoesNotEscape")
    void saveGame_repositoryFailureDoesNotEscape() {
        PlayerGameRepository repository = mock(PlayerGameRepository.class);
        PlayerGameService service = new PlayerGameService(repository);

        GameSession session = mock(GameSession.class);
        WordGame wordGame = mock(WordGame.class);
        Dictionary dictionary = mock(Dictionary.class);
        when(session.getWordGame()).thenReturn(wordGame);
        when(session.getGameId()).thenReturn("game-2");
        when(session.getCurrentAttempts()).thenReturn(6);
        when(session.getMaxAttempts()).thenReturn(6);
        when(wordGame.getTargetWord()).thenReturn("stole");
        when(wordGame.getDictionary()).thenReturn(dictionary);
        when(dictionary.getWordLength()).thenReturn(5);
        when(wordGame.getGuesses()).thenReturn(List.of(response("crane", "RARRA", false)));
        doThrow(new RuntimeException("db failure")).when(repository).save(any());

        assertDoesNotThrow(() -> service.saveGame(null, "203.0.113.9", session, "hard"));
    }

    @Test
    @DisplayName("getHistory_truncatesToMostRecent100Games")
    void getHistory_truncatesToMostRecent100Games() {
        PlayerGameRepository repository = mock(PlayerGameRepository.class);
        PlayerGameService service = new PlayerGameService(repository);

        List<PersistedGame> games = new ArrayList<>();
        for (long index = 1; index <= 101; index++) {
            PersistedGame game = new PersistedGame();
            game.setId(index);
            game.setTargetWord("word" + index);
            game.setWordLength(5);
            game.setDictionaryId("default");
            game.setGuessWords("crane");
            game.setGuessResponses("RARRA");
            game.setResult("LOST");
            game.setAttemptsUsed(6);
            game.setMaxAttempts(6);
            game.setCompletedAt(LocalDateTime.now().minusMinutes(index));
            games.add(game);
        }
        when(repository.findByUserIdOrderByCompletedAtDesc(7L)).thenReturn(games);

        List<GameHistoryDto> history = service.getHistory(7L);

        assertEquals(100, history.size());
        assertEquals(1L, history.getFirst().getId());
        assertEquals(100L, history.getLast().getId());
    }

    @Test
    @DisplayName("getHistory_mapsNullAndDelimitedFields")
    void getHistory_mapsNullAndDelimitedFields() {
        PlayerGameRepository repository = mock(PlayerGameRepository.class);
        PlayerGameService service = new PlayerGameService(repository);

        PersistedGame emptyGame = new PersistedGame();
        emptyGame.setId(11L);
        emptyGame.setTargetWord("arose");
        emptyGame.setWordLength(5);
        emptyGame.setDictionaryId("default");
        emptyGame.setResult("WON");
        emptyGame.setAttemptsUsed(3);
        emptyGame.setMaxAttempts(6);

        PersistedGame populatedGame = new PersistedGame();
        populatedGame.setId(12L);
        populatedGame.setTargetWord("stole");
        populatedGame.setWordLength(5);
        populatedGame.setDictionaryId("hard");
        populatedGame.setGuessWords("crane,stole");
        populatedGame.setGuessResponses("RARRA;GGGGG");
        populatedGame.setResult("WON");
        populatedGame.setAttemptsUsed(2);
        populatedGame.setMaxAttempts(6);
        populatedGame.setCompletedAt(LocalDateTime.of(2026, 4, 25, 12, 30));

        when(repository.findByUserIdOrderByCompletedAtDesc(9L)).thenReturn(List.of(emptyGame, populatedGame));

        List<GameHistoryDto> history = service.getHistory(9L);

        assertTrue(history.get(0).getGuesses().isEmpty());
        assertTrue(history.get(0).getResponses().isEmpty());
        assertNull(history.get(0).getCompletedAt());
        assertEquals(List.of("crane", "stole"), history.get(1).getGuesses());
        assertEquals(List.of("RARRA", "GGGGG"), history.get(1).getResponses());
        assertEquals("2026-04-25T12:30", history.get(1).getCompletedAt());
    }

    @Test
    @DisplayName("countGames_delegatesToRepository")
    void countGames_delegatesToRepository() {
        PlayerGameRepository repository = mock(PlayerGameRepository.class);
        PlayerGameService service = new PlayerGameService(repository);
        when(repository.countByUserId(5L)).thenReturn(27L);

        assertEquals(27L, service.countGames(5L));
    }

    private static Response response(String word, String statuses, boolean winner) {
        Response response = new Response(word);
        for (int index = 0; index < statuses.length(); index++) {
            response.setStatus(word.charAt(index), statuses.charAt(index));
        }
        response.setWinner(winner);
        return response;
    }
}