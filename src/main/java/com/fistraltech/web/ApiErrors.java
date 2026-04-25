package com.fistraltech.web;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Shared helper for the repository-wide REST error contract.
 */
public final class ApiErrors {

    private ApiErrors() {
    }

    public static Map<String, String> body(String error, String message) {
        Map<String, String> response = new HashMap<>();
        response.put("error", error);
        response.put("message", message);
        return response;
    }

    public static ResponseEntity<Map<String, String>> response(
            HttpStatus status, String error, String message) {
        return ResponseEntity.status(status).body(body(error, message));
    }
}