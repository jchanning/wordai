package com.fistraltech.security.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fistraltech.security.dto.UserDto;
import com.fistraltech.security.dto.UserRegistrationDto;
import com.fistraltech.security.service.UserService;

import jakarta.validation.Valid;

/**
 * REST controller for user authentication and registration.
 *
 * <p><strong>Base path</strong>: {@code /api/auth}
 *
 * <p><strong>Endpoints</strong>
 * <ul>
 *   <li>{@code POST /register} — register a new local user account</li>
 *   <li>{@code GET /user} — return the currently authenticated user's profile</li>
 *   <li>{@code POST /logout} — invalidate the current session</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private final UserService userService;
    
    public AuthController(UserService userService) {
        this.userService = userService;
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegistrationDto registrationDto) {
        try {
            UserDto userDto = userService.registerUser(registrationDto);
            return ResponseEntity.ok(userDto);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Registration failed");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    @GetMapping("/user")
    public ResponseEntity<?> getCurrentUser(Authentication authentication,
                                           @AuthenticationPrincipal UserDetails userDetails,
                                           @AuthenticationPrincipal OAuth2User oauth2User) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String email = null;
        String name = null;
        
        if (oauth2User != null) {
            // OAuth2 user
            email = oauth2User.getAttribute("email");
            name = oauth2User.getAttribute("name");
        } else if (userDetails != null) {
            // Form login user
            email = userDetails.getUsername();
        }
        
        if (email != null) {
            return userService.getUserByEmail(email)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    
    @GetMapping("/check")
    public ResponseEntity<?> checkAuth(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", authentication != null && authentication.isAuthenticated());
        return ResponseEntity.ok(response);
    }
}
