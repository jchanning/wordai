package com.fistraltech.server;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import com.fistraltech.security.model.User;
import com.fistraltech.security.repository.UserRepository;
import com.fistraltech.server.dto.GameHistoryDto;
import com.fistraltech.server.model.GameSession;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameHistoryService")
class GameHistoryServiceTest {

    @Mock PlayerGameService playerGameService;
    @Mock UserRepository   userRepository;
    @Mock Authentication   authentication;
    @Mock GameSession      gameSession;

    @InjectMocks
    GameHistoryService service;

    // ---- helpers ----

    private User userWithId(long id, String username) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setEmail(username + "@example.com");
        return u;
    }

    // ============================================================
    // resolveUser
    // ============================================================

    @Test
    @DisplayName("T1: resolveUser — null authentication returns empty")
    void resolveUser_nullAuth_returnsEmpty() {
        assertThat(service.resolveUser(null)).isEmpty();
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("T2: resolveUser — anonymous authentication returns empty")
    void resolveUser_notAuthenticated_returnsEmpty() {
        when(authentication.isAuthenticated()).thenReturn(false);
        assertThat(service.resolveUser(authentication)).isEmpty();
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("T3: resolveUser — found by username")
    void resolveUser_foundByUsername() {
        User user = userWithId(1L, "alice");
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        Optional<User> result = service.resolveUser(authentication);

        assertThat(result).contains(user);
        verify(userRepository).findByUsername("alice");
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    @DisplayName("T4: resolveUser — username not found, falls back to email")
    void resolveUser_usernameNotFound_fallsBackToEmail() {
        User user = userWithId(2L, "bob");
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("bob@example.com");
        when(userRepository.findByUsername("bob@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(user));

        Optional<User> result = service.resolveUser(authentication);

        assertThat(result).contains(user);
        verify(userRepository).findByUsername("bob@example.com");
        verify(userRepository).findByEmail("bob@example.com");
    }

    @Test
    @DisplayName("T5: resolveUser — neither username nor email matches returns empty")
    void resolveUser_noMatch_returnsEmpty() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("ghost");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("ghost")).thenReturn(Optional.empty());

        assertThat(service.resolveUser(authentication)).isEmpty();
    }

    // ============================================================
    // saveIfEnded
    // ============================================================

    @Test
    @DisplayName("T6: saveIfEnded — game not ended is a no-op")
    void saveIfEnded_gameNotEnded_noOp() {
        when(gameSession.isGameEnded()).thenReturn(false);

        service.saveIfEnded(gameSession, authentication);

        verifyNoInteractions(playerGameService, userRepository, authentication);
    }

    @Test
    @DisplayName("T7: saveIfEnded — game ended but null auth is a no-op")
    void saveIfEnded_nullAuth_noOp() {
        when(gameSession.isGameEnded()).thenReturn(true);

        service.saveIfEnded(gameSession, null);

        verifyNoInteractions(playerGameService, userRepository);
    }

    @Test
    @DisplayName("T8: saveIfEnded — game ended but anonymous auth is a no-op")
    void saveIfEnded_notAuthenticated_noOp() {
        when(gameSession.isGameEnded()).thenReturn(true);
        when(authentication.isAuthenticated()).thenReturn(false);

        service.saveIfEnded(gameSession, authentication);

        verifyNoInteractions(playerGameService);
        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    @DisplayName("T9: saveIfEnded — game ended, authenticated, but user not found is a no-op")
    void saveIfEnded_userNotFound_noOp() {
        when(gameSession.isGameEnded()).thenReturn(true);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("ghost");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("ghost")).thenReturn(Optional.empty());

        service.saveIfEnded(gameSession, authentication);

        verifyNoInteractions(playerGameService);
    }

    @Test
    @DisplayName("T10: saveIfEnded — game ended, authenticated, user found → saveGame called")
    void saveIfEnded_gameEnded_userFound_savesGame() {
        User user = userWithId(99L, "admin");
        when(gameSession.isGameEnded()).thenReturn(true);
        when(gameSession.getDictionaryId()).thenReturn("default");
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        service.saveIfEnded(gameSession, authentication);

        verify(playerGameService).saveGame(99L, gameSession, "default");
        verifyNoMoreInteractions(playerGameService);
    }

    // ============================================================
    // getHistory
    // ============================================================

    @Test
    @DisplayName("T11: getHistory — null auth returns empty")
    void getHistory_nullAuth_returnsEmpty() {
        assertThat(service.getHistory(null)).isEmpty();
        verifyNoInteractions(playerGameService);
    }

    @Test
    @DisplayName("T12: getHistory — not authenticated returns empty")
    void getHistory_notAuthenticated_returnsEmpty() {
        when(authentication.isAuthenticated()).thenReturn(false);

        assertThat(service.getHistory(authentication)).isEmpty();
        verifyNoInteractions(playerGameService);
    }

    @Test
    @DisplayName("T13: getHistory — user not found returns empty")
    void getHistory_userNotFound_returnsEmpty() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("nobody");
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("nobody")).thenReturn(Optional.empty());

        assertThat(service.getHistory(authentication)).isEmpty();
        verifyNoInteractions(playerGameService);
    }

    @Test
    @DisplayName("T14: getHistory — user found returns UserHistory with correct username and games")
    void getHistory_userFound_returnsUserHistory() {
        User user = userWithId(7L, "carol");
        List<GameHistoryDto> games = List.of(mock(GameHistoryDto.class), mock(GameHistoryDto.class));

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("carol");
        when(userRepository.findByUsername("carol")).thenReturn(Optional.of(user));
        when(playerGameService.getHistory(7L)).thenReturn(games);

        Optional<GameHistoryService.UserHistory> result = service.getHistory(authentication);

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("carol");
        assertThat(result.get().getGames()).isSameAs(games);
        verify(playerGameService).getHistory(7L);
    }
}
