package com.fistraltech.server.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
