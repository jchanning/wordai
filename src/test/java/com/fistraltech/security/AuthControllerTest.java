package com.fistraltech.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fistraltech.security.config.SecurityConfig;
import com.fistraltech.security.controller.AuthController;
import com.fistraltech.security.exception.DuplicateResourceException;
import com.fistraltech.security.service.CustomOAuth2UserService;
import com.fistraltech.security.service.CustomUserDetailsService;
import com.fistraltech.security.service.UserService;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;

    @Test
    @DisplayName("register_duplicateEmail_returnsConsistentBadRequest")
    void register_duplicateEmail_returnsConsistentBadRequest() throws Exception {
        when(userService.registerUser(any()))
                .thenThrow(new DuplicateResourceException("Email already exists"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {"email":"taken@example.com","username":"takenuser","password":"Password123","fullName":"Taken User"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid request"))
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    @DisplayName("register_blankEmail_returnsValidationErrorBody")
    void register_blankEmail_returnsValidationErrorBody() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {"email":"","username":"validuser","password":"Password123","fullName":"Valid User"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid request"))
                .andExpect(jsonPath("$.message").value("Email is required"));
    }
}