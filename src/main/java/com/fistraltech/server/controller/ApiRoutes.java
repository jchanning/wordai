package com.fistraltech.server.controller;

/**
 * Shared API route constants.
 */
public final class ApiRoutes {

    public static final String LEGACY_ROOT = "/api/wordai";
    public static final String V1_ROOT = "/api/v1/wordai";

    private ApiRoutes() {
        throw new UnsupportedOperationException("Utility class");
    }
}