package com.fistraltech.security.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fistraltech.security.dto.UserDto;
import com.fistraltech.security.dto.UserRegistrationDto;
import com.fistraltech.security.model.User;
import com.fistraltech.security.repository.UserRepository;

@Service
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    public UserDto registerUser(UserRegistrationDto registrationDto) {
        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        
        if (userRepository.existsByUsername(registrationDto.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        
        User user = new User();
        user.setEmail(registrationDto.getEmail());
        user.setUsername(registrationDto.getUsername());
        user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        user.setFullName(registrationDto.getFullName());
        user.setProvider("local");
        user.setCreatedAt(LocalDateTime.now());
        
        User savedUser = userRepository.save(user);
        return convertToDto(savedUser);
    }
    
    public UserDto getOrCreateOAuthUser(String email, String name, String provider, String providerId) {
        Optional<User> existingUser = userRepository.findByProviderAndProviderId(provider, providerId);
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
            return convertToDto(user);
        }
        
        // Create new OAuth user
        User user = new User();
        user.setEmail(email);
        user.setUsername(email.split("@")[0] + "_" + provider);
        user.setFullName(name);
        user.setProvider(provider);
        user.setProviderId(providerId);
        user.setCreatedAt(LocalDateTime.now());
        user.setLastLogin(LocalDateTime.now());
        
        User savedUser = userRepository.save(user);
        return convertToDto(savedUser);
    }
    
    public Optional<UserDto> getUserByEmail(String email) {
        return userRepository.findByEmail(email).map(this::convertToDto);
    }
    
    public Optional<UserDto> getUserById(Long id) {
        return userRepository.findById(id).map(this::convertToDto);
    }

    // Admin methods for user management
    
    @Transactional
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public Page<UserDto> getUsersPaged(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::convertToDto);
    }
    
    @Transactional
    public UserDto updateUserRoles(Long userId, List<String> roles) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.getRoles().clear();
        roles.forEach(user::addRole);
        
        User savedUser = userRepository.save(user);
        return convertToDto(savedUser);
    }
    
    @Transactional
    public UserDto addRoleToUser(Long userId, String role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.addRole(role);
        User savedUser = userRepository.save(user);
        return convertToDto(savedUser);
    }
    
    @Transactional
    public UserDto removeRoleFromUser(Long userId, String role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.removeRole(role);
        User savedUser = userRepository.save(user);
        return convertToDto(savedUser);
    }
    
    @Transactional
    public UserDto enableUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setEnabled(true);
        User savedUser = userRepository.save(user);
        return convertToDto(savedUser);
    }
    
    @Transactional
    public UserDto disableUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setEnabled(false);
        User savedUser = userRepository.save(user);
        return convertToDto(savedUser);
    }
    
    @Transactional
    public void updateUserLastLogin(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
        });
    }
    
    public long getTotalUserCount() {
        return userRepository.count();
    }
    
    public long getActiveUserCount() {
        return userRepository.findAll().stream()
                .filter(User::isEnabled)
                .count();
    }
    
    public List<UserDto> getUsersByRole(String role) {
        return userRepository.findAll().stream()
                .filter(user -> user.hasRole(role))
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    private UserDto convertToDto(User user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getFullName(),
                user.getProvider(),
                user.getRoles(),
                user.getPrimaryRole(),
                user.getCreatedAt(),
                user.getLastLogin(),
                user.isEnabled()
        );
    }
}
