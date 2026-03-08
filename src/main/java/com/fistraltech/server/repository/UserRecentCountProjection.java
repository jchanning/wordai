package com.fistraltech.server.repository;

/**
 * JPQL projection for a per-user count.
 * Reused by multiple windowed counts (7-day, 30-day, won games).
 */
public interface UserRecentCountProjection {

    Long getUserId();

    String getClientIpAddress();

    long getCount();
}
