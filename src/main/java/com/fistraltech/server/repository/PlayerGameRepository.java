package com.fistraltech.server.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fistraltech.server.model.PersistedGame;

/**
 * Spring Data JPA repository for {@link PersistedGame}.
 */
@Repository
public interface PlayerGameRepository extends JpaRepository<PersistedGame, Long> {

    /**
     * Returns all completed games for a user, newest first.
     *
     * @param userId the player's numeric user ID
     * @return list ordered by completedAt descending
     */
    List<PersistedGame> findByUserIdOrderByCompletedAtDesc(Long userId);

    /**
     * Returns a page of completed games for a user.
     *
     * @param userId   the player's numeric user ID
     * @param pageable paging / sorting parameters
     * @return page of persisted games
     */
    Page<PersistedGame> findByUserId(Long userId, Pageable pageable);

    /** Total games played by a user. */
    long countByUserId(Long userId);

    /**
     * Returns aggregate activity stats (total games, first/last game date) for
     * every user who has at least one recorded game. Used by the admin activity screen.
     */
    @Query("SELECT pg.userId AS userId, COUNT(pg) AS totalGames, " +
           "MAX(pg.completedAt) AS lastGameDate, MIN(pg.completedAt) AS firstGameDate " +
           "FROM PersistedGame pg GROUP BY pg.userId")
    List<UserActivityProjection> findUserActivityStats();

    /**
     * Returns per-user game counts for games completed on or after {@code since}.
     * Reused for 7-day and 30-day windows.
     */
    @Query("SELECT pg.userId AS userId, COUNT(pg) AS count " +
           "FROM PersistedGame pg WHERE pg.completedAt >= :since GROUP BY pg.userId")
    List<UserRecentCountProjection> countByUserSince(@Param("since") LocalDateTime since);

    /**
     * Returns per-user counts of games where the result was {@code "WON"}.
     */
    @Query("SELECT pg.userId AS userId, COUNT(pg) AS count " +
           "FROM PersistedGame pg WHERE pg.result = 'WON' GROUP BY pg.userId")
    List<UserRecentCountProjection> countWonGamesByUser();
}
