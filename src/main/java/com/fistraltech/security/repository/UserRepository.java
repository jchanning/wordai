package com.fistraltech.security.repository;

import com.fistraltech.security.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for persisted WordAI user accounts.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Finds a user by email address.
     *
     * @param email user email address
     * @return matching user when present
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Finds a user by username.
     *
     * @param username user name
     * @return matching user when present
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Finds a user by OAuth provider identity.
     *
     * @param provider provider identifier
     * @param providerId provider-scoped user identifier
     * @return matching user when present
     */
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
    
    /**
     * Checks whether a user exists for an email address.
     *
     * @param email user email address
     * @return {@code true} when a matching user exists
     */
    boolean existsByEmail(String email);
    
    /**
     * Checks whether a user exists for a username.
     *
     * @param username user name
     * @return {@code true} when a matching user exists
     */
    boolean existsByUsername(String username);
}
