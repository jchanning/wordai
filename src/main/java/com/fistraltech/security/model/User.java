package com.fistraltech.security.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(unique = true)
    private String username;
    
    @Column
    private String password; // Nullable for OAuth users
    
    @Column(name = "full_name")
    private String fullName;
    
    @Column(name = "provider")
    private String provider; // "local", "google", "apple"
    
    @Column(name = "provider_id")
    private String providerId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<String> roles = new HashSet<>();
    
    @Column(name = "enabled")
    private boolean enabled = true;
    
    // Role constants
    public static final String ROLE_GUEST = "ROLE_GUEST";
    public static final String ROLE_USER = "ROLE_USER"; // Registered Player
    public static final String ROLE_PREMIUM = "ROLE_PREMIUM"; // Premium Player
    public static final String ROLE_ADMIN = "ROLE_ADMIN"; // Administrator
    
    public User() {
        this.createdAt = LocalDateTime.now();
        this.roles.add(ROLE_USER); // Default to registered player
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
    
    public String getProviderId() {
        return providerId;
    }
    
    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getLastLogin() {
        return lastLogin;
    }
    
    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }
    
    public Set<String> getRoles() {
        return roles;
    }
    
    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    // Role checking convenience methods
    public boolean isGuest() {
        return roles.contains(ROLE_GUEST);
    }
    
    public boolean isRegisteredPlayer() {
        return roles.contains(ROLE_USER);
    }
    
    public boolean isPremiumPlayer() {
        return roles.contains(ROLE_PREMIUM);
    }
    
    public boolean isAdmin() {
        return roles.contains(ROLE_ADMIN);
    }
    
    public void addRole(String role) {
        this.roles.add(role);
    }
    
    public void removeRole(String role) {
        this.roles.remove(role);
    }
    
    public boolean hasRole(String role) {
        return roles.contains(role);
    }
    
    public String getPrimaryRole() {
        if (isAdmin()) return "Administrator";
        if (isPremiumPlayer()) return "Premium Player";
        if (isRegisteredPlayer()) return "Registered Player";
        if (isGuest()) return "Guest";
        return "Unknown";
    }
}
