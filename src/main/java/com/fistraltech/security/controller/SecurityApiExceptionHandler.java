package com.fistraltech.security.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fistraltech.security.exception.DuplicateResourceException;
import com.fistraltech.security.exception.InvalidOperationException;
import com.fistraltech.security.exception.ResourceNotFoundException;
import com.fistraltech.web.ApiErrors;

/**
 * Security-controller specific exception mapping that keeps security exceptions inside the security slice.
 */
@RestControllerAdvice(assignableTypes = {AuthController.class, UserManagementController.class})
public class SecurityApiExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFound(ResourceNotFoundException exception) {
        return ApiErrors.response(HttpStatus.NOT_FOUND, "Resource not found", exception.getMessage());
    }

    @ExceptionHandler({DuplicateResourceException.class, InvalidOperationException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException exception) {
        return ApiErrors.response(HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage());
    }
}