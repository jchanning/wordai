# Coding Standards Findings

Date: 2026-04-10

This document evaluates the current WordAI codebase against the agreed standards in `docs/coding-standards.md`, plus the project-specific API and testing conventions under `.github/instructions/`.

Scope of review:
- Production code under `src/main/java`
- Test code under `src/test/java`
- Current JaCoCo report generated via `mvn verify`

The findings below are ordered by impact on correctness, reliability, security, and maintainability.

## Priority Summary

| Priority | Area | Impact | Evidence |
| --- | --- | --- | --- |
| P0 | Input validation at API boundaries | Invalid external input reaches services; inconsistent fail-fast behavior | `AnalysisController`, `WordGameController`, `AnalysisRequest`, `CreateGameRequest`, `GuessRequest` |
| P0 | Exceptions are swallowed or downgraded to console output | Operational failures can be hidden, making faults hard to detect and recover from | `PlayerAnalyser`, `ComplexityAnalyser`, `WordGameService`, `SessionPersistenceService` |
| P1 | Test coverage below agreed minimum | Regression risk remains high despite a working test suite | `target/site/jacoco/index.html` shows 57% line coverage |
| P1 | Error response shape is inconsistent and under-specified | API consumers cannot rely on a stable error contract | `AnalysisController`, `WordGameController`, `AuthController` |
| P2 | Public interface documentation is incomplete | Higher onboarding cost and weaker AI/human maintenance | `Timer`, `AuthController`, `UserDto`, `UserRegistrationDto`, others |
| P2 | Large multi-responsibility classes | Reduced clarity, harder reviews, more fragile changes | `WordGameController`, `WordGameService`, `WordEntropy` |
| P3 | Test conventions are inconsistently applied | Tests are harder to scan and less uniform | `ResponseCacheTest`, `ResponseMatrixTest`, `WordEntropyLazyTest`, others |
| P3 | Encapsulation is inconsistent in some core types | State is easier to misuse and harder to reason about | `EntropyKey` |

## Findings

### P0. Missing validation at API boundaries

Standard references:
- `docs/coding-standards.md`: "All external inputs must be validated and sanitized" and "Fail fast on invalid state; validate inputs at boundaries."
- `.github/instructions/api.instructions.md`: DTOs should validate request data at the boundary.

Evidence:
- `src/main/java/com/fistraltech/server/controller/AnalysisController.java:46` accepts `AnalysisRequest` without `@Valid`.
- `src/main/java/com/fistraltech/server/controller/WordGameController.java:120` accepts `CreateGameRequest` without `@Valid`.
- `src/main/java/com/fistraltech/server/controller/WordGameController.java:171` manually validates `request.getWord()` instead of using bean validation.
- `src/main/java/com/fistraltech/server/dto/AnalysisRequest.java:19` has no validation annotations.
- `src/main/java/com/fistraltech/server/dto/CreateGameRequest.java:24` has no validation annotations.
- `src/main/java/com/fistraltech/server/dto/GuessRequest.java:15` has no validation annotations.
- By contrast, `src/main/java/com/fistraltech/security/controller/AuthController.java:26` and `src/main/java/com/fistraltech/security/dto/UserRegistrationDto.java:7` already use `@Valid`, `@NotBlank`, `@Email`, and `@Size` correctly.

Why correction is required:
- Validation behavior is inconsistent across the API surface.
- Invalid requests are handled ad hoc in controllers instead of by a consistent boundary contract.
- This increases the chance of internal exceptions, divergent error messages, and incomplete sanitization.

Required correction:
- Add Jakarta Bean Validation annotations to request DTOs in `com.fistraltech.server.dto`.
- Use `@Valid` on controller request bodies.
- Move basic request-shape validation out of controller bodies and into DTO constraints.

### P0. Exceptions are swallowed or reduced to non-actionable output

Standard references:
- `docs/coding-standards.md`: "Never swallow exceptions silently."
- `docs/coding-standards.md`: "Log only actionable information."
- `docs/coding-standards.md`: "Use structured logs."

Evidence:
- `src/main/java/com/fistraltech/analysis/PlayerAnalyser.java:124-127` catches `Exception` while running a game and ignores it completely.
- `src/main/java/com/fistraltech/analysis/PlayerAnalyser.java:157` writes failures to `System.err` instead of structured logging.
- `src/main/java/com/fistraltech/analysis/PlayerAnalyser.java:180-183` ignores `IOException` during close.
- `src/main/java/com/fistraltech/analysis/ComplexityAnalyser.java:75-76` and `:101-102` call `printStackTrace()`.
- `src/main/java/com/fistraltech/server/WordGameService.java:469-471` logs reconstruction failure and returns `null`, collapsing the error into an ambiguous "not found" outcome.
- `src/main/java/com/fistraltech/server/SessionPersistenceService.java:71-73`, `:98-100`, `:113-115` catch broad `Exception`, log a warning, and continue after persistence failures.

Why correction is required:
- Faults become difficult to detect and diagnose.
- Persistence and reconstruction failures can leave in-memory and persisted state inconsistent.
- Console output bypasses the project logging approach and cannot be managed consistently in production.

Required correction:
- Replace console output and `printStackTrace()` with `Logger` calls.
- Avoid broad `catch (Exception)` where a smaller exception set is known.
- Re-throw or return a structured failure when the operation cannot complete safely.
- Use try-with-resources instead of manual close blocks that suppress errors.

### P1. Test coverage is materially below the agreed threshold

Standard reference:
- `docs/coding-standards.md`: "Minimum 85% line coverage."

Evidence:
- Current JaCoCo report at `target/site/jacoco/index.html` reports `57%` overall line coverage (`7,696 of 18,152` lines missed).

Why correction is required:
- The codebase has a working suite, but it does not meet the agreed quality bar.
- The gap is large enough that regression detection is weaker than the standard requires.

Required correction:
- Raise coverage toward the 85% minimum, prioritizing server flows, persistence failure paths, and untested utility/security behavior.
- Add regression tests for any bug fix work.

### P1. Error responses are inconsistent and do not meet the documented contract

Standard references:
- `docs/coding-standards.md`: "Error responses must follow a consistent schema."
- `docs/coding-standards.md`: structured errors should include an error code, human-readable message, and contextual metadata.
- `.github/instructions/api.instructions.md`: established API error responses use a stable map with `error` and `message`.

Evidence:
- `src/main/java/com/fistraltech/server/controller/AnalysisController.java:56-64` returns `{ error, message }`, but no error code or metadata.
- `src/main/java/com/fistraltech/server/controller/WordGameController.java:159-168` and `:225-246` build ad hoc maps repeatedly in controller methods.
- `src/main/java/com/fistraltech/security/controller/AuthController.java:28-34` returns only `{ error }` for registration failures, omitting `message` and any stable code.
- `src/main/java/com/fistraltech/server/controller/WordGameController.java:185-189` builds a detailed error body for "game not found" and then returns `ResponseEntity.notFound().build()`, dropping the body entirely.

Why correction is required:
- Clients cannot rely on one error format across the application.
- Missing codes and metadata make client-side handling and server-side diagnostics weaker.
- The current pattern duplicates boilerplate across controllers.

Required correction:
- Introduce a single error DTO or centralized exception handler.
- Standardize on one shape across `server` and `security` controllers.
- Include stable error codes and contextual metadata where appropriate.

### P2. Public interface documentation is incomplete

Standard reference:
- `docs/coding-standards.md`: "Use docstrings for all public interfaces."

Evidence:
- `src/main/java/com/fistraltech/util/Timer.java:3` has no class Javadoc.
- `src/main/java/com/fistraltech/security/controller/AuthController.java:17` has no class Javadoc.
- `src/main/java/com/fistraltech/security/dto/UserRegistrationDto.java:7` has no class Javadoc.
- `src/main/java/com/fistraltech/security/dto/UserDto.java:6` has no class Javadoc.
- Additional public types without class Javadoc found by scan include `EntropyKey`, `DataInitializer`, `User`, `UserRepository`, `CustomOAuth2UserService`, `CustomUserDetailsService`, and `UserService`.

Why correction is required:
- Public interfaces are part of the project contract for both humans and AI tooling.
- Missing Javadocs weaken maintainability and make side effects and invariants less discoverable.

Required correction:
- Add concise Javadocs to all public classes and public API-facing methods that currently lack them.
- Prioritize controllers, DTOs, services, and security types first.

### P2. Some classes exceed the project’s simplicity and separation-of-concerns targets

Standard references:
- `docs/coding-standards.md`: "Keep files short enough to understand at a glance."
- `docs/coding-standards.md`: "Each module, class, and function should have a single, clear responsibility."
- `docs/coding-standards.md`: recommended function size `30-40` lines.

Evidence:
- `src/main/java/com/fistraltech/server/controller/WordGameController.java` is 479 lines.
- `src/main/java/com/fistraltech/server/WordGameService.java` is 481 lines.
- `src/main/java/com/fistraltech/core/WordEntropy.java` is 818 lines.
- `src/main/java/com/fistraltech/server/controller/WordGameController.java:120-168` shows a single endpoint handling request normalization, service orchestration, analytics calculation, DTO assembly, and error mapping.
- `src/main/java/com/fistraltech/server/controller/WordGameController.java:176-246` repeats the same multi-responsibility pattern for guess handling.

Why correction is required:
- Oversized classes increase change risk and review cost.
- Mixed responsibilities make testing and reuse harder.
- Controllers are doing data-shaping and analytics work that would be easier to test in dedicated collaborators.

Required correction:
- Split controller assembly logic from service orchestration.
- Extract analytics-to-response mapping into dedicated assemblers/helpers.
- Break large classes into narrower domain units before adding more behavior.

### P3. Test conventions are inconsistently applied

Standard references:
- `docs/coding-standards.md`: tests should be deterministic, isolated, and readable.
- `.github/instructions/testing.instructions.md`: class-level `@DisplayName` is always present.

Evidence:
- The following test classes contain no `@DisplayName` annotations at all:
  - `src/test/java/com/fistraltech/analysis/ResponseCacheTest.java`
  - `src/test/java/com/fistraltech/analysis/ResponseMatrixTest.java`
  - `src/test/java/com/fistraltech/analysis/WordEntropyLazyTest.java`
  - `src/test/java/com/fistraltech/bot/selection/SelectBellmanFullDictionaryRepeatedGuessTest.java`
  - `src/test/java/com/fistraltech/bot/selection/SelectBellmanFullDictionaryTest.java`

Why correction is required:
- Tests are harder to scan in IDE and CI output.
- The suite is less uniform than the agreed test conventions require.

Required correction:
- Add class-level and test-level `@DisplayName` annotations where missing.
- Use the established naming format consistently across the remaining test classes.

### P3. Encapsulation is inconsistent in some domain types

Standard references:
- `docs/coding-standards.md`: "Encapsulation — hide internal details and expose only necessary interfaces."
- `docs/coding-standards.md`: "Avoid global mutable state" and implicit behavior.

Evidence:
- `src/main/java/com/fistraltech/analysis/EntropyKey.java:3-5` exposes package-private mutable fields (`guessedWord`, `response`) with no constructor, no validation, and no encapsulation.

Why correction is required:
- The type can be mutated from elsewhere in the package without any invariants being enforced.
- Equality and ordering rely on mutable state, which makes the object risky to use in maps or sets if modified after insertion.

Required correction:
- Make fields private and final.
- Add a constructor that validates required values.
- Keep `equals`, `hashCode`, and `compareTo` based on immutable state.

## Recommended Fix Order

1. Add DTO validation and `@Valid` usage across the server controllers.
2. Replace swallowed exceptions, `System.out/err`, and `printStackTrace()` with structured logging and explicit failure handling.
3. Standardize API error responses through a shared error model or global exception handler.
4. Raise test coverage toward 85%, starting with the unvalidated request flows and persistence failure paths.
5. Add missing Javadocs for public interfaces in `security`, `util`, and remaining DTOs.
6. Refactor oversized controller/service classes into smaller collaborators.
7. Normalize remaining test conventions and tighten encapsulation on legacy utility/domain classes.