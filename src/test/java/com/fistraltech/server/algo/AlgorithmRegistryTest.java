package com.fistraltech.server.algo;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fistraltech.bot.selection.SelectBellmanFullDictionary;
import com.fistraltech.bot.selection.SelectMaximumEntropy;
import com.fistraltech.bot.selection.SelectRandom;
import com.fistraltech.bot.selection.SelectionAlgo;
import com.fistraltech.core.Dictionary;

/**
 * Unit tests for {@link AlgorithmRegistry}.
 */
@DisplayName("AlgorithmRegistry Tests")
class AlgorithmRegistryTest {

    private AlgorithmRegistry registry;
    private Dictionary dictionary;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        registry = AlgorithmRegistry.withDefaults();

        dictionary = new Dictionary(5);
        Set<String> words = new HashSet<>();
        words.add("arose");
        words.add("stare");
        words.add("crane");
        words.add("slate");
        words.add("raise");
        dictionary.addWords(words);
    }

    // ---- T1-T3: known IDs return correct concrete types ----

    @Test
    @DisplayName("T1: create(RANDOM) returns a SelectRandom instance")
    void create_random_returnsSelectRandom() {
        SelectionAlgo algo = registry.create("RANDOM", dictionary);
        assertNotNull(algo);
        assertTrue(algo instanceof SelectRandom,
                "Expected SelectRandom but got " + algo.getClass().getSimpleName());
    }

    @Test
    @DisplayName("T2: create(ENTROPY) returns a SelectMaximumEntropy instance")
    void create_entropy_returnsSelectMaximumEntropy() {
        SelectionAlgo algo = registry.create("ENTROPY", dictionary);
        assertNotNull(algo);
        assertTrue(algo instanceof SelectMaximumEntropy,
                "Expected SelectMaximumEntropy but got " + algo.getClass().getSimpleName());
    }

    @Test
    @DisplayName("T3: create(BELLMAN_FULL_DICTIONARY) returns a SelectBellmanFullDictionary instance")
    void create_bellman_returnsSelectBellmanFullDictionary() {
        SelectionAlgo algo = registry.create("BELLMAN_FULL_DICTIONARY", dictionary);
        assertNotNull(algo);
        assertTrue(algo instanceof SelectBellmanFullDictionary,
                "Expected SelectBellmanFullDictionary but got " + algo.getClass().getSimpleName());
    }

    // ---- T4: unknown ID falls back to RANDOM ----

    @Test
    @DisplayName("T4: create(UNKNOWN) falls back to SelectRandom")
    void create_unknownId_fallsBackToRandom() {
        SelectionAlgo algo = registry.create("NOT_A_REAL_ALGO", dictionary);
        assertNotNull(algo);
        assertTrue(algo instanceof SelectRandom,
                "Unknown ID should fall back to SelectRandom");
    }

    @Test
    @DisplayName("T4b: create(null) falls back to SelectRandom")
    void create_nullId_fallsBackToRandom() {
        SelectionAlgo algo = registry.create(null, dictionary);
        assertNotNull(algo);
        assertTrue(algo instanceof SelectRandom,
                "null ID should fall back to SelectRandom");
    }

    // ---- T5: different instances returned on repeated calls ----

    @Test
    @DisplayName("T5: consecutive create() calls return distinct instances")
    void create_calledTwice_returnsDifferentInstances() {
        SelectionAlgo first = registry.create("RANDOM", dictionary);
        SelectionAlgo second = registry.create("RANDOM", dictionary);
        assertNotSame(first, second, "Each create() call must return a new instance");
    }

    // ---- T6: isStateful() returns correct values ----

    @Test
    @DisplayName("T6a: isStateful(RANDOM) returns false")
    void isStateful_random_returnsFalse() {
        assertFalse(registry.isStateful("RANDOM"));
    }

    @Test
    @DisplayName("T6b: isStateful(ENTROPY) returns false")
    void isStateful_entropy_returnsFalse() {
        assertFalse(registry.isStateful("ENTROPY"));
    }

    @Test
    @DisplayName("T6c: isStateful(BELLMAN_FULL_DICTIONARY) returns true")
    void isStateful_bellman_returnsTrue() {
        assertTrue(registry.isStateful("BELLMAN_FULL_DICTIONARY"));
    }

    @Test
    @DisplayName("T6d: isStateful(unknown) returns false")
    void isStateful_unknown_returnsFalse() {
        assertFalse(registry.isStateful("DOES_NOT_EXIST"));
    }

    // ---- IDs are case-insensitive ----

    @Test
    @DisplayName("ID lookup is case-insensitive")
    void create_caseInsensitive_succeeds() {
        SelectionAlgo lower = registry.create("random", dictionary);
        SelectionAlgo mixed = registry.create("Entropy", dictionary);
        assertTrue(lower instanceof SelectRandom);
        assertTrue(mixed instanceof SelectMaximumEntropy);
    }

    // ---- getRegisteredIds() coverage ----

    @Test
    @DisplayName("getRegisteredIds() includes all three default algorithms")
    void getRegisteredIds_includesDefaults() {
        Set<String> ids = registry.getRegisteredIds();
        assertTrue(ids.contains("RANDOM"));
        assertTrue(ids.contains("ENTROPY"));
        assertTrue(ids.contains("BELLMAN_FULL_DICTIONARY"));
        assertEquals(3, ids.size());
    }

    @Test
    @DisplayName("descriptor metadata is exposed from the registry")
    void getDescriptor_exposesMetadata() {
        AlgorithmDescriptor descriptor = registry.getDescriptor("BELLMAN_FULL_DICTIONARY").orElseThrow();
        assertEquals("3 Expert — Bellman Optimality", descriptor.getDisplayName());
        assertEquals("Minimises expected guesses across the full dictionary — strongest, most thorough", descriptor.getDescription());
        assertEquals("bellman-full-dictionary", descriptor.getFeatureToggleKey());
        assertTrue(descriptor.isStateful());
    }

    @Test
    @DisplayName("normalizeId maps aliases to canonical IDs")
    void normalizeId_alias_returnsCanonicalId() {
        assertEquals("ENTROPY", registry.normalizeId("MAXIMUM_ENTROPY"));
        assertEquals("RANDOM", registry.normalizeId(null));
    }
}
