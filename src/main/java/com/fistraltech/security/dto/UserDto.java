package com.fistraltech.security.dto;

import java.util.Set;
import java.time.LocalDateTime;

public class UserDto {
    
    private Long id;
    private String email;
    private String username;
    private String fullName;
    private String provider;
    private Set<String> roles;
    private String primaryRole;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private boolean enabled;
    
    // Constructors
    public UserDto() {
    }
    
    public UserDto(Long id, String email, String username, String fullName, String provider, 
                   Set<String> roles, String primaryRole, LocalDateTime createdAt, 
                   LocalDateTime lastLogin, boolean enabled) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.fullName = fullName;
        this.provider = provider;
        this.roles = roles;
        this.primaryRole = primaryRole;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
        this.enabled = enabled;
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

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public String getPrimaryRole() {
        return primaryRole;
    }

    public void setPrimaryRole(String primaryRole) {
        this.primaryRole = primaryRole;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    // Role checking convenience methods
    public boolean isGuest() {
        return roles != null && roles.contains("ROLE_GUEST");
    }

    public boolean isRegisteredPlayer() {
        return roles != null && roles.contains("ROLE_USER");
    }

    public boolean isPremiumPlayer() {
        return roles != null && roles.contains("ROLE_PREMIUM");
    }

    public boolean isAdmin() {
        return roles != null && roles.contains("ROLE_ADMIN");
    }

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}
