package com.fistraltech.security.controller;

import com.fistraltech.security.dto.UserDto;
import com.fistraltech.security.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for user management (Admin only)
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "*")
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
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        try {
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
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to retrieve users: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Get user by ID
     * GET /api/admin/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Update user roles
     * PUT /api/admin/users/{id}/roles
     */
    @PutMapping("/{id}/roles")
    public ResponseEntity<?> updateUserRoles(@PathVariable Long id, @RequestBody List<String> roles) {
        try {
            UserDto updatedUser = userService.updateUserRoles(id, roles);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    /**
     * Add role to user
     * POST /api/admin/users/{id}/roles/{role}
     */
    @PostMapping("/{id}/roles/{role}")
    public ResponseEntity<?> addRoleToUser(@PathVariable Long id, @PathVariable String role) {
        try {
            UserDto updatedUser = userService.addRoleToUser(id, role);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    /**
     * Remove role from user
     * DELETE /api/admin/users/{id}/roles/{role}
     */
    @DeleteMapping("/{id}/roles/{role}")
    public ResponseEntity<?> removeRoleFromUser(@PathVariable Long id, @PathVariable String role) {
        try {
            UserDto updatedUser = userService.removeRoleFromUser(id, role);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    /**
     * Enable user
     * PUT /api/admin/users/{id}/enable
     */
    @PutMapping("/{id}/enable")
    public ResponseEntity<?> enableUser(@PathVariable Long id) {
        try {
            UserDto updatedUser = userService.enableUser(id);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    /**
     * Disable user
     * PUT /api/admin/users/{id}/disable
     */
    @PutMapping("/{id}/disable")
    public ResponseEntity<?> disableUser(@PathVariable Long id) {
        try {
            UserDto updatedUser = userService.disableUser(id);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    /**
     * Get users by role
     * GET /api/admin/users/role/{role}
     */
    @GetMapping("/role/{role}")
    public ResponseEntity<List<UserDto>> getUsersByRole(@PathVariable String role) {
        try {
            List<UserDto> users = userService.getUsersByRole(role);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get user statistics
     * GET /api/admin/users/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getUserStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalUsers", userService.getTotalUserCount());
            stats.put("activeUsers", userService.getActiveUserCount());
            stats.put("adminUsers", userService.getUsersByRole("ROLE_ADMIN").size());
            stats.put("premiumUsers", userService.getUsersByRole("ROLE_PREMIUM").size());
            stats.put("registeredUsers", userService.getUsersByRole("ROLE_USER").size());
            stats.put("guestSessions", userService.getUsersByRole("ROLE_GUEST").size());
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to retrieve user statistics: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}