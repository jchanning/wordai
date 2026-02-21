---
description: 'Refactor a class or method. Provide the target class and the goal of the refactor.'
---

Refactor the following WordAI code.

## Target
<!-- Fill in before running this prompt -->
**Class / method:** [e.g. `com.fistraltech.core.DictionaryManager`]
**Goal:** [e.g. Extract dictionary loading into a separate strategy class]

## Rules for this refactor

### Do
- Read the target class fully before changing anything.
- Check all callers with a codebase search before changing a method signature.
- Maintain identical observable behaviour — the refactor must not change what the code does, only how it does it.
- Keep the same package structure unless the refactor explicitly moves a class.
- Run the full test suite after each logical step, not once at the end.

### Do not
- Do not change test code to make a failing test pass.
- Do not introduce new dependencies (libraries) without asking.
- Do not break the layer hierarchy (`server` ← `analysis` ← `bot` ← `core` ← `util`).
- Do not add Lombok, records, or sealed classes unless the Java 21 target has been confirmed in context.

## Priority targets for known violations (optional — pick one)
These are documented in `ArchitectureFitnessTest` as `@Disabled` tests:

1. **Move `DictionaryOption` out of `server.dto`** → move to `com.fistraltech.core` or `com.fistraltech.util`. Update all imports. This enables the `core_mustNotDependOn_server`, `util_mustNotDependOn_server` fitness tests.

2. **Move `AnalysisGameResult` + `AnalysisResponse` out of `server.dto`** → move to `com.fistraltech.analysis`. Update `PlayerAnalyser` and its callers. This enables the `analysis_mustNotDependOn_server` fitness test.

3. **Break the `core` ↔ `bot` cycle** → `Dictionary` imports `FilterCharacters` and `ResponseHelper` imports `Filter`. Options: (a) move `FilterCharacters`/`Filter` to `core`; (b) extract an interface in `core` that `bot` implements. This enables the `noCyclicPackageDependencies` fitness test.

## Verification
After completing the refactor:
```
mvn clean test
```
All 219+ tests must still pass. If a previously `@Disabled` fitness test can now be enabled, re-enable it.
