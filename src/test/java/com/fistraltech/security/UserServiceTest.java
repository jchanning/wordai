package com.fistraltech.security;

import com.fistraltech.security.model.User;
import com.fistraltech.security.repository.UserRepository;
import com.fistraltech.security.service.UserService;
import com.fistraltech.security.dto.UserDto;
import com.fistraltech.security.dto.UserRegistrationDto;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User createTestUser(Long id, String email, String username, String provider) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setUsername(username);
        user.setFullName("Test User");
        user.setProvider(provider);
        user.setCreatedAt(LocalDateTime.now());
        user.setEnabled(true);
        user.addRole("ROLE_USER");
        return user;
    }

    @Nested
    @DisplayName("Registration Tests")
    class Registration {
        @Test
        @DisplayName("should register a new user successfully")
        void registerUser_success() {
            UserRegistrationDto dto = new UserRegistrationDto("test@example.com", "testuser", "password123", "Test User");
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });

            UserDto result = userService.registerUser(dto);

            assertNotNull(result);
            assertEquals("test@example.com", result.getEmail());
            assertEquals("testuser", result.getUsername());
        }

        @Test
        @DisplayName("should reject duplicate email")
        void registerUser_duplicateEmail() {
            UserRegistrationDto dto = new UserRegistrationDto("dup@example.com", "user", "pass1234", "N");
            when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

            assertThrows(RuntimeException.class, () -> userService.registerUser(dto));
        }

        @Test
        @DisplayName("should reject duplicate username")
        void registerUser_duplicateUsername() {
            UserRegistrationDto dto = new UserRegistrationDto("x@example.com", "taken", "pass1234", "N");
            when(userRepository.existsByEmail("x@example.com")).thenReturn(false);
            when(userRepository.existsByUsername("taken")).thenReturn(true);

            assertThrows(RuntimeException.class, () -> userService.registerUser(dto));
        }
    }

    @Nested
    @DisplayName("Role Management Tests")
    class RoleManagement {
        @Test
        @DisplayName("should add a role to user")
        void addRole() {
            User user = createTestUser(1L, "t@e.com", "t", "local");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UserDto result = userService.addRoleToUser(1L, "ROLE_ADMIN");

            assertTrue(result.getRoles().contains("ROLE_ADMIN"));
        }

        @Test
        @DisplayName("should remove a role from user")
        void removeRole() {
            User user = createTestUser(1L, "t@e.com", "t", "local");
            user.addRole("ROLE_ADMIN");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UserDto result = userService.removeRoleFromUser(1L, "ROLE_ADMIN");

            assertFalse(result.getRoles().contains("ROLE_ADMIN"));
        }

        @Test
        @DisplayName("should throw when adding role to nonexistent user")
        void addRole_userNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());
            assertThrows(RuntimeException.class, () -> userService.addRoleToUser(99L, "ROLE_ADMIN"));
        }

        @Test
        @DisplayName("should throw when removing role from nonexistent user")
        void removeRole_userNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());
            assertThrows(RuntimeException.class, () -> userService.removeRoleFromUser(99L, "ROLE_ADMIN"));
        }

        @Test
        @DisplayName("should bulk update user roles")
        void updateRoles() {
            User user = createTestUser(1L, "t@e.com", "t", "local");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UserDto result = userService.updateUserRoles(1L, List.of("ROLE_ADMIN", "ROLE_PREMIUM"));

            assertTrue(result.getRoles().contains("ROLE_ADMIN"));
            assertTrue(result.getRoles().contains("ROLE_PREMIUM"));
        }
    }

    @Nested
    @DisplayName("Enable/Disable Tests")
    class EnableDisable {
        @Test
        @DisplayName("should disable user")
        void disableUser() {
            User user = createTestUser(1L, "t@e.com", "t", "local");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UserDto result = userService.disableUser(1L);

            assertFalse(result.isEnabled());
        }

        @Test
        @DisplayName("should enable user")
        void enableUser() {
            User user = createTestUser(1L, "t@e.com", "t", "local");
            user.setEnabled(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UserDto result = userService.enableUser(1L);

            assertTrue(result.isEnabled());
        }
    }

    @Nested
    @DisplayName("Get All Users Tests")
    class GetAllUsers {
        @Test
        @DisplayName("should return all users as DTOs")
        void getAllUsers() {
            User u1 = createTestUser(1L, "a@e.com", "a", "local");
            User u2 = createTestUser(2L, "b@e.com", "b", "google");
            when(userRepository.findAll()).thenReturn(List.of(u1, u2));

            List<UserDto> result = userService.getAllUsers();

            assertEquals(2, result.size());
        }
    }

    @Nested
    @DisplayName("DTO Role Mapping")
    class DtoRoleMapping {
        @Test
        @DisplayName("should include roles and enabled in DTO")
        void convertToDto_includesRolesAndEnabled() {
            User user = createTestUser(1L, "t@e.com", "t", "local");
            user.addRole("ROLE_ADMIN");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            Optional<UserDto> result = userService.getUserById(1L);

            assertTrue(result.isPresent());
            assertTrue(result.get().getRoles().contains("ROLE_ADMIN"));
            assertTrue(result.get().getRoles().contains("ROLE_USER"));
            assertTrue(result.get().isEnabled());
        }
    }

    @Nested
    @DisplayName("Password Reset Tests")
    class PasswordReset {
        @Test
        @DisplayName("should reset password for local user")
        void resetPassword_success() {
            User user = createTestUser(1L, "t@e.com", "t", "local");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("NewPass123!")).thenReturn("encoded_new");
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UserDto result = userService.resetPassword(1L, "NewPass123!");

            assertNotNull(result);
            verify(passwordEncoder).encode("NewPass123!");
            verify(userRepository).save(any());
        }

        @Test
        @DisplayName("should reject password reset for OAuth user")
        void resetPassword_oauthUser() {
            User user = createTestUser(1L, "t@e.com", "t", "google");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            assertThrows(IllegalStateException.class,
                    () -> userService.resetPassword(1L, "NewPass123!"));
        }

        @Test
        @DisplayName("should reject short password")
        void resetPassword_tooShort() {
            User user = createTestUser(1L, "t@e.com", "t", "local");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            assertThrows(IllegalArgumentException.class,
                    () -> userService.resetPassword(1L, "short"));
        }

        @Test
        @DisplayName("should throw when user not found")
        void resetPassword_userNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                    () -> userService.resetPassword(99L, "NewPass123!"));
        }
    }
}
