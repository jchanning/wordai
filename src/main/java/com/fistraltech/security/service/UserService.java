package com.fistraltech.security.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fistraltech.security.dto.UserDto;
import com.fistraltech.security.dto.UserRegistrationDto;
import com.fistraltech.security.exception.DuplicateResourceException;
import com.fistraltech.security.exception.InvalidOperationException;
import com.fistraltech.security.exception.ResourceNotFoundException;
import com.fistraltech.security.model.User;
import com.fistraltech.security.repository.UserRepository;

/**
 * Application service for user registration, lookup, and administrative account management.
 *
 * <p>This service owns the main mutation surface for local and OAuth-backed user records,
 * including role management, status toggles, password resets, and user-facing DTO mapping.
 */
@Service
public class UserService {

    private static final String USER_NOT_FOUND = "User not found";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    /**
     * Registers a new locally authenticated user.
     *
     * @param registrationDto validated registration request
     * @return created user profile
     * @throws DuplicateResourceException if the email or username is already in use
     */
    public UserDto registerUser(UserRegistrationDto registrationDto) {
        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }
        
        if (userRepository.existsByUsername(registrationDto.getUsername())) {
            throw new DuplicateResourceException("Username already exists");
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
    
    /**
     * Loads an existing OAuth user or creates one on first login.
     *
     * @param email provider email address
     * @param name provider display name
     * @param provider OAuth provider identifier
     * @param providerId provider-scoped user identifier
     * @return resolved user profile
     */
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
    
    /**
     * Finds a user profile by email address.
     *
     * @param email user email address
     * @return matching user profile when present
     */
    public Optional<UserDto> getUserByEmail(String email) {
        return userRepository.findByEmail(email).map(this::convertToDto);
    }
    
    /**
     * Finds a user profile by database identifier.
     *
     * @param id user identifier
     * @return matching user profile when present
     */
    public Optional<UserDto> getUserById(@NonNull Long id) {
        return userRepository.findById(id).map(this::convertToDto);
    }

    /**
     * Returns all persisted users for administrative views.
     *
     * @return all users mapped to DTOs
     */
    @Transactional
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDto)
                .toList();
    }
    
    /**
     * Returns a paged view of users for administrative views.
     *
     * @param pageable paging and sorting options
     * @return paged user DTOs
     */
    @Transactional
    public Page<UserDto> getUsersPaged(@NonNull Pageable pageable) {
        return userRepository.findAll(pageable).map(this::convertToDto);
    }
    
    /**
     * Replaces the full role set for a user.
     *
     * @param userId target user identifier
     * @param roles replacement role names
     * @return updated user profile
     * @throws ResourceNotFoundException if the user does not exist
     */
    @Transactional
    public UserDto updateUserRoles(@NonNull Long userId, List<String> roles) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
        
        user.getRoles().clear();
        roles.forEach(user::addRole);
        
        User savedUser = userRepository.save(user);
        return convertToDto(savedUser);
    }
    
    /**
     * Adds one role to a user.
     *
     * @param userId target user identifier
     * @param role role to add
     * @return updated user profile
     * @throws ResourceNotFoundException if the user does not exist
     */
    @Transactional
    public UserDto addRoleToUser(@NonNull Long userId, String role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
        
        user.addRole(role);
        User savedUser = userRepository.save(user);
        return convertToDto(savedUser);
    }
    
    /**
     * Removes one role from a user.
     *
     * @param userId target user identifier
     * @param role role to remove
     * @return updated user profile
     * @throws ResourceNotFoundException if the user does not exist
     */
    @Transactional
    public UserDto removeRoleFromUser(@NonNull Long userId, String role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
        
        user.removeRole(role);
        User savedUser = userRepository.save(user);
        return convertToDto(savedUser);
    }
    
    /**
     * Enables a user account.
     *
     * @param userId target user identifier
     * @return updated user profile
     * @throws ResourceNotFoundException if the user does not exist
     */
    @Transactional
    public UserDto enableUser(@NonNull Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
        
        user.setEnabled(true);
        User savedUser = userRepository.save(user);
        return convertToDto(savedUser);
    }
    
    /**
     * Disables a user account.
     *
     * @param userId target user identifier
     * @return updated user profile
     * @throws ResourceNotFoundException if the user does not exist
     */
    @Transactional
    public UserDto disableUser(@NonNull Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
        
        user.setEnabled(false);
        User savedUser = userRepository.save(user);
        return convertToDto(savedUser);
    }
    
    /**
     * Updates the last-login timestamp for the user with the given email address.
     *
     * @param email user email address
     */
    @Transactional
    public void updateUserLastLogin(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
        });
    }
    
    /**
     * Counts all persisted users.
     *
     * @return total user count
     */
    public long getTotalUserCount() {
        return userRepository.count();
    }
    
    /**
     * Counts enabled users.
     *
     * @return number of enabled users
     */
    public long getActiveUserCount() {
        return userRepository.findAll().stream()
                .filter(User::isEnabled)
                .count();
    }
    
    /**
     * Returns users who currently hold a specific role.
     *
     * @param role role name to filter by
     * @return matching user profiles
     */
    public List<UserDto> getUsersByRole(String role) {
        return userRepository.findAll().stream()
                .filter(user -> user.hasRole(role))
                .map(this::convertToDto)
                .toList();
    }
    
    /**
     * Resets the password for a local user account.
     *
     * @param userId target user identifier
     * @param newPassword replacement password
     * @return updated user profile
     * @throws ResourceNotFoundException if the user does not exist
     * @throws InvalidOperationException if the user is OAuth-backed or the password is invalid
     */
    @Transactional
    public UserDto resetPassword(@NonNull Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
        
        if (!"local".equals(user.getProvider())) {
            throw new InvalidOperationException("Cannot reset password for OAuth users");
        }
        
        if (newPassword == null || newPassword.length() < 8) {
            throw new InvalidOperationException("Password must be at least 8 characters");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        User savedUser = userRepository.save(user);
        return convertToDto(savedUser);
    }
    
    /**
     * Checks whether a user already exists for an email address.
     *
     * @param email user email address
     * @return {@code true} when a user exists for the email
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
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
