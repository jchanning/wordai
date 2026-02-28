package com.fistraltech.server.algo;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fistraltech.bot.selection.SelectionAlgo;
import com.fistraltech.core.Dictionary;

/**
 * Spring-managed registry of all registered word-selection algorithms.
 *
 * <p>All beans implementing {@link AlgorithmDescriptor} are auto-discovered via
 * constructor injection. To register a new algorithm, add a {@code @Component}
 * class that implements {@link AlgorithmDescriptor} — no changes to this class
 * or to {@link com.fistraltech.server.WordGameService} are required.
 *
 * <p><strong>Key property</strong>: {@link #create(String, Dictionary)} always
 * returns a <em>fresh</em> instance. Callers that need per-game stateful instances
 * (see {@link #isStateful(String)}) are responsible for caching the returned object.
 *
 * @see AlgorithmDescriptor
 */
@Component
public class AlgorithmRegistry {

    private static final Logger logger = Logger.getLogger(AlgorithmRegistry.class.getName());
    private static final String DEFAULT_ID = "RANDOM";

    private final Map<String, AlgorithmDescriptor> descriptors;

    /**
     * Spring constructor — receives all {@link AlgorithmDescriptor} beans in the context.
     *
     * @param all the list of registered algorithm descriptors
     */
    public AlgorithmRegistry(List<AlgorithmDescriptor> all) {
        this.descriptors = all.stream()
                .collect(Collectors.toUnmodifiableMap(
                        d -> d.getId().toUpperCase(),
                        d -> d));
        logger.info(() -> "AlgorithmRegistry initialised with: " + descriptors.keySet());
    }

    /**
     * Creates a fresh algorithm instance for the given ID and dictionary.
     *
     * <p>If {@code algorithmId} is {@code null} or unknown, falls back to
     * {@value #DEFAULT_ID}.
     *
     * @param algorithmId the algorithm identifier (case-insensitive)
     * @param dictionary  the game dictionary for the new instance
     * @return a newly created {@link SelectionAlgo}
     */
    public SelectionAlgo create(String algorithmId, Dictionary dictionary) {
        String id = algorithmId != null ? algorithmId.toUpperCase() : DEFAULT_ID;
        AlgorithmDescriptor descriptor = descriptors.getOrDefault(id, descriptors.get(DEFAULT_ID));
        if (descriptor == null) {
            throw new IllegalStateException(
                    "AlgorithmRegistry has no RANDOM descriptor — registry is misconfigured");
        }
        return descriptor.create(dictionary);
    }

    /**
     * Returns {@code true} if the named algorithm is stateful and therefore requires
     * a cached instance per game session. Unknown IDs return {@code false}.
     */
    public boolean isStateful(String algorithmId) {
        if (algorithmId == null) {
            return false;
        }
        AlgorithmDescriptor descriptor = descriptors.get(algorithmId.toUpperCase());
        return descriptor != null && descriptor.isStateful();
    }

    /**
     * Returns the set of all registered algorithm IDs (upper-case).
     * Used by the algorithms endpoint to advertise available strategies.
     */
    public Set<String> getRegisteredIds() {
        return descriptors.keySet();
    }

    /**
     * Factory method that creates a registry pre-loaded with the three default
     * algorithm descriptors. Intended for use in unit tests and the package-private
     * {@link com.fistraltech.server.WordGameService} test constructor.
     */
    public static AlgorithmRegistry withDefaults() {
        return new AlgorithmRegistry(List.of(
                new RandomAlgorithmDescriptor(),
                new EntropyAlgorithmDescriptor(),
                new BellmanFullDictAlgorithmDescriptor()));
    }
}
