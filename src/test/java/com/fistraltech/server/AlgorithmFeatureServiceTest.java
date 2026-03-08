package com.fistraltech.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import com.fistraltech.server.algo.AlgorithmRegistry;

/**
 * Unit tests for {@link AlgorithmFeatureService}.
 */
@DisplayName("AlgorithmFeatureService Tests")
class AlgorithmFeatureServiceTest {

    @Test
    @DisplayName("isAlgorithmEnabled_usesDescriptorDerivedPropertyKeys")
    void isAlgorithmEnabled_usesDescriptorDerivedPropertyKeys() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("algorithm.features.random.enabled", "false")
                .withProperty("algorithm.features.entropy.enabled", "true")
                .withProperty("algorithm.features.bellman-full-dictionary.enabled", "false");
        AlgorithmFeatureService service = new AlgorithmFeatureService(AlgorithmRegistry.withDefaults(), environment);

        assertFalse(service.isAlgorithmEnabled("RANDOM"));
        assertTrue(service.isAlgorithmEnabled("MAXIMUM_ENTROPY"));
        assertFalse(service.isAlgorithmEnabled("BELLMAN_FULL_DICTIONARY"));
    }

    @Test
    @DisplayName("getAllAlgorithms_includesMetadataAndState")
    void getAllAlgorithms_includesMetadataAndState() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("algorithm.features.bellman-full-dictionary.enabled", "false");
        AlgorithmFeatureService service = new AlgorithmFeatureService(AlgorithmRegistry.withDefaults(), environment);

        AlgorithmFeatureService.AlgorithmInfo bellman = service.getAllAlgorithms().get("BELLMAN_FULL_DICTIONARY");
        assertEquals("Bellman Full Dictionary", bellman.getDisplayName());
        assertEquals("algorithm.features.bellman-full-dictionary.enabled", bellman.getFeatureProperty());
        assertTrue(bellman.isStateful());
        assertFalse(bellman.isEnabled());
    }
}