package com.fistraltech.security.dto;

public class UserDto {
    
    private Long id;
    private String email;
    private String username;
    private String fullName;
    private String provider;
    
    // Constructors
    public UserDto() {
    }
    
    public UserDto(Long id, String email, String username, String fullName, String provider) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.fullName = fullName;
        this.provider = provider;
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
}
