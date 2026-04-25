package com.fistraltech.security.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fistraltech.security.dto.UserDto;
import com.fistraltech.security.service.UserService;
import com.fistraltech.web.ApiErrors;

/**
 * REST controller for user management (Admin only)
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserManagementController {
    
    private final UserService userService;
    
    public UserManagementController(UserService userService) {
        this.userService = userService;
    }
    
    /**
     * Get all users with pagination
     * GET /api/admin/users?page=0&size=20&sort=createdAt,desc
     */
    @GetMapping
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        Page<UserDto> users = userService.getUsersPaged(pageRequest);

        Map<String, Object> response = new HashMap<>();
        response.put("users", users.getContent());
        response.put("currentPage", users.getNumber());
        response.put("totalItems", users.getTotalElements());
        response.put("totalPages", users.getTotalPages());
        response.put("hasNext", users.hasNext());
        response.put("hasPrevious", users.hasPrevious());

        return ResponseEntity.ok(response);
    }
    
    /**
     * Get user by ID
     * GET /api/admin/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        UserDto user = userService.getUserById(id).orElse(null);
        if (user != null) {
            return ResponseEntity.ok(user);
        }
        return ApiErrors.response(HttpStatus.NOT_FOUND,
            "User not found", "User " + id + " does not exist");
    }
    
    /**
     * Update user roles
     * PUT /api/admin/users/{id}/roles
     */
    @PutMapping("/{id}/roles")
    public ResponseEntity<?> updateUserRoles(@PathVariable Long id, @RequestBody List<String> roles) {
        UserDto updatedUser = userService.updateUserRoles(id, roles);
        return ResponseEntity.ok(updatedUser);
    }
    
    /**
     * Add role to user
     * POST /api/admin/users/{id}/roles/{role}
     */
    @PostMapping("/{id}/roles/{role}")
    public ResponseEntity<?> addRoleToUser(@PathVariable Long id, @PathVariable String role) {
        UserDto updatedUser = userService.addRoleToUser(id, role);
        return ResponseEntity.ok(updatedUser);
    }
    
    /**
     * Remove role from user
     * DELETE /api/admin/users/{id}/roles/{role}
     */
    @DeleteMapping("/{id}/roles/{role}")
    public ResponseEntity<?> removeRoleFromUser(@PathVariable Long id, @PathVariable String role) {
        UserDto updatedUser = userService.removeRoleFromUser(id, role);
        return ResponseEntity.ok(updatedUser);
    }
    
    /**
     * Enable user
     * PUT /api/admin/users/{id}/enable
     */
    @PutMapping("/{id}/enable")
    public ResponseEntity<?> enableUser(@PathVariable Long id) {
        UserDto updatedUser = userService.enableUser(id);
        return ResponseEntity.ok(updatedUser);
    }
    
    /**
     * Disable user
     * PUT /api/admin/users/{id}/disable
     */
    @PutMapping("/{id}/disable")
    public ResponseEntity<?> disableUser(@PathVariable Long id) {
        UserDto updatedUser = userService.disableUser(id);
        return ResponseEntity.ok(updatedUser);
    }
    
    /**
     * Get users by role
     * GET /api/admin/users/role/{role}
     */
    @GetMapping("/role/{role}")
    public ResponseEntity<?> getUsersByRole(@PathVariable String role) {
        List<UserDto> users = userService.getUsersByRole(role);
        return ResponseEntity.ok(users);
    }
    
    /**
     * Get user statistics
     * GET /api/admin/users/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getUserStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userService.getTotalUserCount());
        stats.put("activeUsers", userService.getActiveUserCount());
        stats.put("adminUsers", userService.getUsersByRole("ROLE_ADMIN").size());
        stats.put("premiumUsers", userService.getUsersByRole("ROLE_PREMIUM").size());
        stats.put("registeredUsers", userService.getUsersByRole("ROLE_USER").size());
        stats.put("guestSessions", userService.getUsersByRole("ROLE_GUEST").size());

        return ResponseEntity.ok(stats);
    }
    
    /**
     * Reset user password (local users only)
     * PUT /api/admin/users/{id}/password
     */
    @PutMapping("/{id}/password")
    public ResponseEntity<?> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String newPassword = body.get("password");
        if (newPassword == null || newPassword.isBlank()) {
            return ApiErrors.response(HttpStatus.BAD_REQUEST,
                    "Invalid request", "Password is required");
        }

        UserDto updated = userService.resetPassword(id, newPassword);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Password reset successfully");
        response.put("user", updated);
        return ResponseEntity.ok(response);
    }
}