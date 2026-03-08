package com.fistraltech.server.repository;

import java.time.LocalDateTime;

/**
 * JPQL projection for per-user game activity aggregate statistics.
 * Returned by {@link PlayerGameRepository#findUserActivityStats()}.
 *
 * <p>Spring Data instantiates this proxy at query execution time; do not
 * add any fields or behaviour here — getters only.
 */
public interface UserActivityProjection {

    Long getUserId();

    String getClientIpAddress();

    long getTotalGames();

    LocalDateTime getLastGameDate();

    LocalDateTime getFirstGameDate();
}
