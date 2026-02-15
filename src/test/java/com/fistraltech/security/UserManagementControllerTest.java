package com.fistraltech.security;

import com.fistraltech.security.config.SecurityConfig;
import com.fistraltech.security.controller.UserManagementController;
import com.fistraltech.security.dto.UserDto;
import com.fistraltech.security.service.CustomOAuth2UserService;
import com.fistraltech.security.service.CustomUserDetailsService;
import com.fistraltech.security.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserManagementController.class)
@Import(SecurityConfig.class)
class UserManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;

    private UserDto testUser() {
        return new UserDto(1L, "test@example.com", "testuser", "Test User", "local",
                Set.of("ROLE_USER"), "ROLE_USER", LocalDateTime.now(), null, true);
    }

    // === Access control tests ===

    @Test
    @DisplayName("should deny unauthenticated access")
    void unauthenticated_denied() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("should deny non-admin access")
    void nonAdmin_denied() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("should allow admin to list users")
    void admin_listUsers() throws Exception {
        Page<UserDto> page = new PageImpl<>(List.of(testUser()));
        when(userService.getUsersPaged(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users", hasSize(1)))
                .andExpect(jsonPath("$.users[0].username", is("testuser")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("should allow admin to get user by id")
    void admin_getUserById() throws Exception {
        when(userService.getUserById(1L)).thenReturn(Optional.of(testUser()));

        mockMvc.perform(get("/api/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("testuser")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("should return 404 for nonexistent user")
    void admin_getUserById_notFound() throws Exception {
        when(userService.getUserById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/admin/users/99"))
                .andExpect(status().isNotFound());
    }

    // === Role management tests ===

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("should allow admin to add a role")
    void admin_addRole() throws Exception {
        UserDto updated = new UserDto(1L, "test@example.com", "testuser", "Test User", "local",
                Set.of("ROLE_USER", "ROLE_ADMIN"), "ROLE_ADMIN", LocalDateTime.now(), null, true);
        when(userService.addRoleToUser(eq(1L), eq("ADMIN"))).thenReturn(updated);

        mockMvc.perform(post("/api/admin/users/1/roles/ADMIN")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", hasItem("ROLE_ADMIN")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("should allow admin to remove a role")
    void admin_removeRole() throws Exception {
        UserDto updated = new UserDto(1L, "test@example.com", "testuser", "Test User", "local",
                Set.of("ROLE_USER"), "ROLE_USER", LocalDateTime.now(), null, true);
        when(userService.removeRoleFromUser(eq(1L), eq("ADMIN"))).thenReturn(updated);

        mockMvc.perform(delete("/api/admin/users/1/roles/ADMIN")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", not(hasItem("ROLE_ADMIN"))));
    }

    // === Enable/disable tests ===

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("should allow admin to disable a user")
    void admin_disableUser() throws Exception {
        UserDto updated = new UserDto(1L, "test@example.com", "testuser", "Test User", "local",
                Set.of("ROLE_USER"), "ROLE_USER", LocalDateTime.now(), null, false);
        when(userService.disableUser(eq(1L))).thenReturn(updated);

        mockMvc.perform(put("/api/admin/users/1/disable")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled", is(false)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("should allow admin to enable a user")
    void admin_enableUser() throws Exception {
        UserDto updated = new UserDto(1L, "test@example.com", "testuser", "Test User", "local",
                Set.of("ROLE_USER"), "ROLE_USER", LocalDateTime.now(), null, true);
        when(userService.enableUser(eq(1L))).thenReturn(updated);

        mockMvc.perform(put("/api/admin/users/1/enable")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled", is(true)));
    }

    // === Password reset tests ===

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("should allow admin to reset password")
    void admin_resetPassword() throws Exception {
        when(userService.resetPassword(eq(1L), eq("NewPass123!"))).thenReturn(testUser());

        mockMvc.perform(put("/api/admin/users/1/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\": \"NewPass123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Password reset successfully")))
                .andExpect(jsonPath("$.user.username", is("testuser")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("should return 400 when resetting password for OAuth user")
    void admin_resetPassword_oauthUser() throws Exception {
        when(userService.resetPassword(eq(2L), eq("NewPass123!")))
                .thenThrow(new IllegalStateException("Cannot reset password for OAuth users"));

        mockMvc.perform(put("/api/admin/users/2/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\": \"NewPass123!\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("should deny non-admin password reset")
    void nonAdmin_resetPassword_denied() throws Exception {
        mockMvc.perform(put("/api/admin/users/1/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\": \"NewPass123!\"}"))
                .andExpect(status().isForbidden());
    }
}
