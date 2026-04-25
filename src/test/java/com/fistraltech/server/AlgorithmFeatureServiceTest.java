package com.fistraltech.server;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.mock.env.MockEnvironment;

import com.fistraltech.server.algo.AlgorithmRegistry;
import com.fistraltech.server.model.AlgorithmPolicyEntity;
import com.fistraltech.server.repository.AlgorithmPolicyRepository;

/**
 * Unit tests for {@link AlgorithmFeatureService}.
 */
@DisplayName("AlgorithmFeatureService Tests")
class AlgorithmFeatureServiceTest {

    @Test
    @DisplayName("isAlgorithmEnabled_usesDescriptorDerivedPropertyKeys")
    void isAlgorithmEnabled_usesDescriptorDerivedPropertyKeys() {
        AlgorithmPolicyRepository repository = mock(AlgorithmPolicyRepository.class);
        MockEnvironment environment = new MockEnvironment()
                .withProperty("algorithm.features.random.enabled", "false")
                .withProperty("algorithm.features.entropy.enabled", "true")
                .withProperty("algorithm.features.bellman-full-dictionary.enabled", "false");
        when(repository.findById("RANDOM")).thenReturn(Optional.empty());
        when(repository.findById("BELLMAN_FULL_DICTIONARY")).thenReturn(Optional.empty());

        AlgorithmPolicyEntity entropyPolicy = new AlgorithmPolicyEntity();
        entropyPolicy.setAlgorithmId("ENTROPY");
        entropyPolicy.setEnabled(false);
        when(repository.findById("ENTROPY")).thenReturn(Optional.of(entropyPolicy));

        AlgorithmFeatureService service = new AlgorithmFeatureService(
            AlgorithmRegistry.withDefaults(), environment, repository, Clock.systemUTC());

        assertFalse(service.isAlgorithmEnabled("RANDOM"));
        assertFalse(service.isAlgorithmEnabled("MAXIMUM_ENTROPY"));
        assertFalse(service.isAlgorithmEnabled("BELLMAN_FULL_DICTIONARY"));
    }

    @Test
    @DisplayName("getAllAlgorithms_includesMetadataAndState")
    void getAllAlgorithms_includesMetadataAndState() {
        AlgorithmPolicyRepository repository = mock(AlgorithmPolicyRepository.class);
        MockEnvironment environment = new MockEnvironment()
                .withProperty("algorithm.features.bellman-full-dictionary.enabled", "false");
        when(repository.findById("RANDOM")).thenReturn(Optional.empty());
        when(repository.findById("ENTROPY")).thenReturn(Optional.empty());
        when(repository.findById("BELLMAN_FULL_DICTIONARY")).thenReturn(Optional.empty());

        AlgorithmFeatureService service = new AlgorithmFeatureService(
            AlgorithmRegistry.withDefaults(), environment, repository, Clock.systemUTC());

        AlgorithmFeatureService.AlgorithmInfo bellman = service.getAllAlgorithms().get("BELLMAN_FULL_DICTIONARY");
        assertEquals("Expert — Bellman Optimality", bellman.getDisplayName());
        assertEquals("algorithm.features.bellman-full-dictionary.enabled", bellman.getFeatureProperty());
        assertEquals(true, bellman.isStateful());
        assertFalse(bellman.isEnabled());
    }

        @Test
        @DisplayName("updateAlgorithmEnabled_persistsRuntimePolicy")
        void updateAlgorithmEnabled_persistsRuntimePolicy() {
        AlgorithmPolicyRepository repository = mock(AlgorithmPolicyRepository.class);
        MockEnvironment environment = new MockEnvironment();
        Clock clock = Clock.fixed(Instant.parse("2026-04-25T12:00:00Z"), ZoneOffset.UTC);
        when(repository.findById("ENTROPY")).thenReturn(Optional.empty());

        AlgorithmFeatureService service = new AlgorithmFeatureService(
            AlgorithmRegistry.withDefaults(), environment, repository, clock);

        AlgorithmFeatureService.AlgorithmInfo updated = service.updateAlgorithmEnabled("MAXIMUM_ENTROPY", false);

        assertEquals("ENTROPY", updated.getId());
        assertFalse(updated.isEnabled());
        verify(repository).save(argThat(policy ->
            "ENTROPY".equals(policy.getAlgorithmId())
                && !policy.isEnabled()
                && LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).equals(policy.getUpdatedAt())));
        }
}