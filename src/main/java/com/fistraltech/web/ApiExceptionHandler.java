package com.fistraltech.web;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Centralised REST exception mapping for server and security controllers.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger logger = Logger.getLogger(ApiExceptionHandler.class.getName());

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<java.util.Map<String, String>> handleValidation(
            MethodArgumentNotValidException exception) {
        FieldError firstError = exception.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String message = firstError != null ? firstError.getDefaultMessage() : "Request validation failed";
        return ApiErrors.response(HttpStatus.BAD_REQUEST, "Invalid request", message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<java.util.Map<String, String>> handleUnreadableBody(
            HttpMessageNotReadableException exception) {
        return ApiErrors.response(HttpStatus.BAD_REQUEST, "Invalid request", "Malformed request body");
    }

    @ExceptionHandler(ApiResourceNotFoundException.class)
    public ResponseEntity<java.util.Map<String, String>> handleApiResourceNotFound(
            ApiResourceNotFoundException exception) {
        return ApiErrors.response(HttpStatus.NOT_FOUND, exception.getError(), exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<java.util.Map<String, String>> handleBadRequest(RuntimeException exception) {
        return ApiErrors.response(HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<java.util.Map<String, String>> handleNoResourceFound(
            NoResourceFoundException exception) {
        return ApiErrors.response(HttpStatus.NOT_FOUND, "Not found", "Requested route does not exist");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<java.util.Map<String, String>> handleUnexpected(Exception exception) {
        logger.log(Level.SEVERE, "Unhandled controller exception", exception);
        String message = exception.getMessage() != null && !exception.getMessage().isBlank()
                ? exception.getMessage()
                : "Request could not be completed";
        return ApiErrors.response(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", message);
    }
}