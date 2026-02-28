package com.fistraltech.server.algo;

import com.fistraltech.bot.selection.SelectionAlgo;
import com.fistraltech.core.Dictionary;

/**
 * Descriptor for a word-selection algorithm.
 *
 * <p>Implementations are Spring {@code @Component} beans auto-discovered by
 * {@link AlgorithmRegistry}. Adding a new algorithm requires only a new descriptor
 * class — no changes to existing code.
 *
 * <p><strong>Stateful algorithms</strong>: some algorithms (e.g.
 * {@code SelectBellmanFullDictionary}) maintain state across successive
 * {@code selectWord()} calls within a game (e.g. a set of already-guessed words).
 * For these, {@link #isStateful()} returns {@code true} and the registry caller is
 * responsible for caching the instance returned by {@link #create(Dictionary)} for
 * the lifetime of the game session.
 *
 * <p>Descriptor beans are singletons; algorithm instances produced by
 * {@link #create(Dictionary)} must always be <em>fresh</em> objects.
 *
 * @see AlgorithmRegistry
 */
public interface AlgorithmDescriptor {

    /**
     * The algorithm identifier (upper-case), e.g. {@code "RANDOM"}, {@code "ENTROPY"}.
     * Must be unique across all registered descriptors.
     */
    String getId();

    /**
     * Creates a fresh algorithm instance bound to the given dictionary.
     * Every invocation must return a distinct object — never a cached singleton.
     */
    SelectionAlgo create(Dictionary dictionary);

    /**
     * Returns {@code true} if this algorithm accumulates per-game state across
     * multiple {@code selectWord()} invocations and therefore requires a cached
     * instance per game session.
     *
     * <p>Stateless algorithms may be created fresh on every suggestion request.
     */
    boolean isStateful();
}
