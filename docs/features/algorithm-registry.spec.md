# Algorithm Registry (Improvement #8)

*Status: implemented in v1.11.0*

## Problem

`WordGameService.createSelectionAlgorithm()` and `GameSession.getCachedOrCreateAlgorithm()` both
contain a `switch` on a string ID that directly instantiates concrete algorithm classes. Every new
algorithm requires editing two different files.

## Design

### AlgorithmDescriptor (interface)

Each algorithm is described by a Spring `@Component` that implements `AlgorithmDescriptor`:

```
String getId()            — upper-case registered ID, e.g. "RANDOM"
SelectionAlgo create(Dictionary) — returns a *fresh* instance per call
boolean isStateful()      — true if the instance must be cached per game session
                            (i.e. it accumulates state across suggestWord() calls)
```

Because algorithms may hold per-session state (e.g. `SelectBellmanFullDictionary.guessedWords`),
descriptors are **singletons** but the instances they produce are **per-game**.

### AlgorithmRegistry (Spring @Component)

Auto-discovers all `AlgorithmDescriptor` beans via constructor injection:

```java
public AlgorithmRegistry(List<AlgorithmDescriptor> all) { ... }
```

Public API:
- `create(String id, Dictionary)` — delegates to matching descriptor; falls back to RANDOM
- `isStateful(String id)` — returns descriptor's `isStateful()`
- `getRegisteredIds()` — set of registered IDs

### Registered algorithms

| ID | Class | Stateful |
|----|-------|---------|
| RANDOM | SelectRandom | false |
| ENTROPY | SelectMaximumEntropy | false |
| BELLMAN_FULL_DICTIONARY | SelectBellmanFullDictionary | true |

### Access change to SelectionAlgo

`selectWord(Response, Dictionary)` is widened from package-private to `public` so that `GameSession`
(in a different package) can call it directly on a cached stateful algorithm without casting to the
concrete type.

## Acceptance tests (AlgorithmRegistryTest)

- T1: `create("RANDOM", dict)` returns a `SelectRandom` instance
- T2: `create("ENTROPY", dict)` returns a `SelectMaximumEntropy` instance
- T3: `create("BELLMAN_FULL_DICTIONARY", dict)` returns a `SelectBellmanFullDictionary` instance
- T4: `create("UNKNOWN", dict)` falls back to `SelectRandom`
- T5: `create()` called twice with the same ID returns **different** object instances
- T6: `isStateful("RANDOM")` → false; `isStateful("BELLMAN_FULL_DICTIONARY")` → true
