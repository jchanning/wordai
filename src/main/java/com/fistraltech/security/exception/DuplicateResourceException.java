package com.fistraltech.security.exception;

/**
 * Thrown when attempting to create a resource that already exists (e.g. duplicate email or username).
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
