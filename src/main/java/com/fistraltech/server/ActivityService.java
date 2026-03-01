package com.fistraltech.server;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fistraltech.security.model.User;
import com.fistraltech.security.repository.UserRepository;
import com.fistraltech.server.dto.UserActivityDto;
import com.fistraltech.server.repository.PlayerGameRepository;
import com.fistraltech.server.repository.UserActivityProjection;
import com.fistraltech.server.repository.UserRecentCountProjection;

/**
 * Service that aggregates per-user game activity statistics for the admin dashboard.
 *
 * <p>Uses four efficient aggregate queries against {@code player_games} to avoid
 * loading individual game rows, then does a single bulk user lookup. Total DB
 * round-trips are O(1) regardless of the number of users or games.
 *
 * <p>Method: {@link #getUserActivityStats()} returns results sorted by most recent
 * activity descending so the admin sees the most active users at the top.
 */
@Service
public class ActivityService {

    private static final Logger logger = Logger.getLogger(ActivityService.class.getName());

    private final PlayerGameRepository gameRepository;
    private final UserRepository userRepository;

    public ActivityService(PlayerGameRepository gameRepository, UserRepository userRepository) {
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
    }

    /**
     * Returns activity statistics for every user who has completed at least one game,
     * sorted by {@code lastGameDate} descending (most recently active first).
     *
     * @return list of per-user activity DTOs
     */
    public List<UserActivityDto> getUserActivityStats() {
        // 1. All-time aggregate per user (1 query)
        List<UserActivityProjection> allStats = gameRepository.findUserActivityStats();

        if (allStats.isEmpty()) {
            return List.of();
        }

        // 2. Recent-game counts (2 queries with time windows)
        LocalDateTime now = LocalDateTime.now();
        Map<Long, Long> last7  = toCountMap(gameRepository.countByUserSince(now.minusDays(7)));
        Map<Long, Long> last30 = toCountMap(gameRepository.countByUserSince(now.minusDays(30)));

        // 3. Won-game counts (1 query)
        Map<Long, Long> wonMap = toCountMap(gameRepository.countWonGamesByUser());

        // 4. Bulk user lookup (1 query)
        List<Long> userIds = allStats.stream()
                .map(UserActivityProjection::getUserId)
                .collect(Collectors.toList());
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 5. Assemble DTOs
        return allStats.stream()
                .map(stat -> {
                    UserActivityDto dto = new UserActivityDto();
                    dto.setUserId(stat.getUserId());

                    User user = userMap.get(stat.getUserId());
                    if (user != null) {
                        dto.setUsername(user.getUsername());
                        dto.setEmail(user.getEmail());
                        dto.setLastLogin(user.getLastLogin());
                    }

                    dto.setTotalGames(stat.getTotalGames());
                    dto.setWonGames(wonMap.getOrDefault(stat.getUserId(), 0L));
                    dto.setGamesLast7Days(last7.getOrDefault(stat.getUserId(), 0L));
                    dto.setGamesLast30Days(last30.getOrDefault(stat.getUserId(), 0L));
                    dto.setLastGameDate(stat.getLastGameDate());
                    dto.setFirstGameDate(stat.getFirstGameDate());
                    return dto;
                })
                .sorted(Comparator.comparing(
                        UserActivityDto::getLastGameDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Map<Long, Long> toCountMap(List<UserRecentCountProjection> projections) {
        return projections.stream()
                .collect(Collectors.toMap(
                        UserRecentCountProjection::getUserId,
                        UserRecentCountProjection::getCount));
    }
}
