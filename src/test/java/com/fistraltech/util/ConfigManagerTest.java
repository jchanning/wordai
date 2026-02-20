package com.fistraltech.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ConfigManager} – singleton behaviour, classpath resolution,
 * and property fallback.
 */
@DisplayName("ConfigManager Tests")
class ConfigManagerTest {

    @Test
    @DisplayName("getInstance returns a non-null instance")
    void getInstance_returnsNonNull() {
        ConfigManager mgr = ConfigManager.getInstance();
        assertNotNull(mgr, "ConfigManager.getInstance() must not return null");
    }

    @Test
    @DisplayName("getInstance returns the same object on every call (singleton)")
    void getInstance_returnsSameInstance() {
        ConfigManager first  = ConfigManager.getInstance();
        ConfigManager second = ConfigManager.getInstance();
        assertSame(first, second, "ConfigManager must be a singleton");
    }

    @Test
    @DisplayName("getProperty with default returns supplied default for an unknown key")
    void getProperty_unknownKey_returnsDefault() {
        ConfigManager mgr = ConfigManager.getInstance();
        String result = mgr.getProperty("this.key.does.not.exist", "fallback");
        assertEquals("fallback", result, "Unknown key should return the supplied default");
    }

    @Test
    @DisplayName("getProperty with default returns existing value for a known key")
    void getProperty_knownKey_returnsValue() {
        ConfigManager mgr = ConfigManager.getInstance();
        // game.word.length is present in application.properties with value 5
        String value = mgr.getProperty("game.word.length", "MISSING");
        assertNotNull(value, "game.word.length should be set in application.properties");
        assertFalse("MISSING".equals(value), "game.word.length should resolve to a real value");
    }

    @Test
    @DisplayName("getProperty: game.max.attempts is a positive integer string")
    void getProperty_maxAttempts_isPositiveInteger() {
        ConfigManager mgr = ConfigManager.getInstance();
        String raw = mgr.getProperty("game.max.attempts", "0");
        int value = Integer.parseInt(raw);
        org.junit.jupiter.api.Assertions.assertTrue(value > 0,
            "game.max.attempts should be positive, got: " + value);
    }

    @Test
    @DisplayName("setProperty and getProperty round-trip correctly")
    void setProperty_roundTrip() {
        ConfigManager mgr = ConfigManager.getInstance();
        mgr.setProperty("test.round.trip", "expectedValue");
        String result = mgr.getProperty("test.round.trip", "default");
        assertEquals("expectedValue", result, "setProperty value should be readable via getProperty");
        // Clean up to avoid polluting other tests
        mgr.setProperty("test.round.trip", "");
    }
}
