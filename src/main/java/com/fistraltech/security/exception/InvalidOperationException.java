package com.fistraltech.security.exception;

/**
 * Thrown when an operation is not valid for the current state of a resource
 * (e.g. resetting a password for an OAuth user).
 */
public class InvalidOperationException extends RuntimeException {

    public InvalidOperationException(String message) {
        super(message);
    }
}
