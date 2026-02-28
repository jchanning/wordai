package com.fistraltech.server.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fistraltech.server.model.ActiveGameSessionEntity;

/**
 * Spring Data JPA repository for {@link ActiveGameSessionEntity}.
 */
@Repository
public interface ActiveGameSessionRepository extends JpaRepository<ActiveGameSessionEntity, String> {

    /**
     * Finds all ACTIVE sessions for a given user.
     *
     * @param userId the player's numeric user ID
     * @param status session status, typically {@code "ACTIVE"}
     * @return list of matching sessions
     */
    List<ActiveGameSessionEntity> findByUserIdAndStatus(Long userId, String status);

    /**
     * Finds an ACTIVE session for a user + dictionary combination.
     * Used to detect whether the user already has an in-progress game
     * for the requested dictionary, enabling session reconstruction on reconnect.
     *
     * @param userId       the player's numeric user ID
     * @param dictionaryId the dictionary identifier
     * @param status       session status, typically {@code "ACTIVE"}
     * @return the matching session, or empty if none found
     */
    Optional<ActiveGameSessionEntity> findByUserIdAndDictionaryIdAndStatus(
            Long userId, String dictionaryId, String status);
}
