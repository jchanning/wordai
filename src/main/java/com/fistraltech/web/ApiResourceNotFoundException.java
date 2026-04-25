package com.fistraltech.web;

/**
 * Signals that a requested API resource does not exist.
 */
public class ApiResourceNotFoundException extends RuntimeException {

    private final String error;

    public ApiResourceNotFoundException(String error, String message) {
        super(message);
        this.error = error;
    }

    public String getError() {
        return error;
    }
}