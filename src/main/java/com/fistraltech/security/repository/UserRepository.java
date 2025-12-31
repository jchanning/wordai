package com.fistraltech.security.repository;

import com.fistraltech.security.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
    
    boolean existsByEmail(String email);
    
    boolean existsByUsername(String username);
}
