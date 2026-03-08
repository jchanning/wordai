package com.fistraltech.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fistraltech.security.model.User;
import com.fistraltech.security.repository.UserRepository;
import com.fistraltech.server.dto.UserActivityDto;
import com.fistraltech.server.repository.PlayerGameRepository;
import com.fistraltech.server.repository.UserActivityProjection;
import com.fistraltech.server.repository.UserRecentCountProjection;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActivityService Tests")
class ActivityServiceTest {

    @Mock
    private PlayerGameRepository gameRepository;

    @Mock
    private UserRepository userRepository;

    private ActivityService service;

    @BeforeEach
    void setUp() {
        service = new ActivityService(gameRepository, userRepository);
    }

    @Test
    @DisplayName("getUserActivityStats_mixedAuthenticatedAndAnonymousPlayers_returnsUnknownIpRows")
    void getUserActivityStats_mixedAuthenticatedAndAnonymousPlayers_returnsUnknownIpRows() {
        LocalDateTime now = LocalDateTime.now();

        UserActivityProjection authenticated = userActivityProjection(7L, null, 4L,
                now.minusDays(2), now.minusDays(30));
        UserActivityProjection anonymous = userActivityProjection(null, "203.0.113.7", 3L,
                now.minusHours(6), now.minusDays(4));

        when(gameRepository.findUserActivityStats()).thenReturn(List.of(authenticated, anonymous));
        when(gameRepository.countByUserSince(org.mockito.ArgumentMatchers.any(LocalDateTime.class))).thenReturn(List.of(
                countProjection(7L, null, 2L),
                countProjection(null, "203.0.113.7", 1L)));
        when(gameRepository.countWonGamesByUser()).thenReturn(List.of(
                countProjection(7L, null, 3L),
                countProjection(null, "203.0.113.7", 1L)));

        User user = new User();
        user.setId(7L);
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        when(userRepository.findAllById(List.of(7L))).thenReturn(List.of(user));

        List<UserActivityDto> result = service.getUserActivityStats();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUsername()).isEqualTo("Unknown");
        assertThat(result.get(0).getIpAddress()).isEqualTo("203.0.113.7");
        assertThat(result.get(0).getEmail()).isNull();
        assertThat(result.get(0).getTotalGames()).isEqualTo(3L);
        assertThat(result.get(0).getGamesLast7Days()).isEqualTo(1L);
        assertThat(result.get(0).getGamesLast30Days()).isEqualTo(1L);
        assertThat(result.get(0).getWonGames()).isEqualTo(1L);

        assertThat(result.get(1).getUsername()).isEqualTo("alice");
        assertThat(result.get(1).getEmail()).isEqualTo("alice@example.com");
        assertThat(result.get(1).getIpAddress()).isNull();
        assertThat(result.get(1).getTotalGames()).isEqualTo(4L);
        assertThat(result.get(1).getGamesLast7Days()).isEqualTo(2L);
        assertThat(result.get(1).getGamesLast30Days()).isEqualTo(2L);
        assertThat(result.get(1).getWonGames()).isEqualTo(3L);
    }

    @Test
    @DisplayName("getUserActivityStats_noPersistedGames_returnsEmptyList")
    void getUserActivityStats_noPersistedGames_returnsEmptyList() {
        when(gameRepository.findUserActivityStats()).thenReturn(List.of());

        List<UserActivityDto> result = service.getUserActivityStats();

        assertThat(result).isEmpty();
        verifyNoInteractions(userRepository);
    }

    private UserActivityProjection userActivityProjection(Long userId, String clientIpAddress, long totalGames,
            LocalDateTime lastGameDate, LocalDateTime firstGameDate) {
        return new UserActivityProjection() {
            @Override
            public Long getUserId() {
                return userId;
            }

            @Override
            public String getClientIpAddress() {
                return clientIpAddress;
            }

            @Override
            public long getTotalGames() {
                return totalGames;
            }

            @Override
            public LocalDateTime getLastGameDate() {
                return lastGameDate;
            }

            @Override
            public LocalDateTime getFirstGameDate() {
                return firstGameDate;
            }
        };
    }

    private UserRecentCountProjection countProjection(Long userId, String clientIpAddress, long count) {
        return new UserRecentCountProjection() {
            @Override
            public Long getUserId() {
                return userId;
            }

            @Override
            public String getClientIpAddress() {
                return clientIpAddress;
            }

            @Override
            public long getCount() {
                return count;
            }
        };
    }
}
