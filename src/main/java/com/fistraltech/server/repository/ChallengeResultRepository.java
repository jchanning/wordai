package com.fistraltech.server.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fistraltech.server.model.ChallengeResultEntity;

/**
 * Repository for completed Challenge Mode runs.
 */
@Repository
public interface ChallengeResultRepository extends JpaRepository<ChallengeResultEntity, Long> {
    Page<ChallengeResultEntity> findAllByOrderByTotalScoreDescCompletedAtAsc(Pageable pageable);
}
