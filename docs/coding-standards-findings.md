# Coding Standards Findings

Date: 2026-04-10

This document evaluates the current WordAI codebase against the agreed standards in `docs/coding-standards.md`, plus the project-specific API and testing conventions under `.github/instructions/`.

Scope of review:
- Production code under `src/main/java`
- Test code under `src/test/java`
- Current JaCoCo report generated via `mvn verify`

The findings below are ordered by impact on correctness, reliability, security, and maintainability.

## Backlog Mapping

The active remediation backlog for the open findings below now lives in [specs/README.md](../specs/README.md). Current ticket mapping:

| Finding area | Active ticket |
| --- | --- |
| Input validation at API boundaries | `ARCH-17` completed |
| Exceptions swallowed / failure handling | `ARCH-18` completed |
| Coverage threshold and high-risk regression gaps | `ARCH-16` completed, `ARCH-19` completed |
| Error response consistency | `ARCH-18` completed |
| Entropy memoization for repeated filter states | `ARCH-22` completed |
| Runtime algorithm policy for operational toggles | `ARCH-23` completed |
| API version boundary for external contracts | `ARCH-24` completed |
| Public interface documentation debt | `ARCH-25` completed |
| Oversized `WordGameController` / `WordGameService` responsibilities | `ARCH-26` |
| Legacy `WordEntropy` execution paths | `ARCH-20` completed |
| `GameSession` decomposition | `ARCH-21` completed |
| Test convention cleanup | `ARCH-27` completed |
| `EntropyKey` encapsulation | `ARCH-28` completed |

## Priority Summary

| Priority | Area | Impact | Evidence |
| --- | --- | --- | --- |
| P0 | Input validation at API boundaries | Invalid external input reaches services; inconsistent fail-fast behavior | `AnalysisController`, `WordGameController`, `AnalysisRequest`, `CreateGameRequest`, `GuessRequest` |
| P0 | Exceptions are swallowed or downgraded to console output | Operational failures can be hidden, making faults hard to detect and recover from | `PlayerAnalyser`, `ComplexityAnalyser`, `WordGameService`, `SessionPersistenceService` |
| P1 | Test coverage below agreed minimum | Regression risk is reduced materially, but the codebase still sits below the long-term 85% target | `target/site/jacoco/index.html` shows 72% line coverage |
| P1 | Error response shape is inconsistent and under-specified | API consumers cannot rely on a stable error contract | `AnalysisController`, `WordGameController`, `AuthController` |
| P2 | Public interface documentation is incomplete | Higher onboarding cost and weaker AI/human maintenance | `Timer`, `AuthController`, `UserDto`, `UserRegistrationDto`, others |
| P2 | Large multi-responsibility classes | Reduced clarity, harder reviews, more fragile changes | `WordGameController`, `WordGameService`, `WordEntropy` |
| P3 | Test conventions are inconsistently applied | Tests are harder to scan and less uniform | `ResponseCacheTest`, `ResponseMatrixTest`, `WordEntropyLazyTest`, others |
| P3 | Encapsulation is inconsistent in some core types | State is easier to misuse and harder to reason about | `EntropyKey` |

## Findings

### P0. Missing validation at API boundaries

Current status (2026-04-25): resolved by `ARCH-17` for the live server request DTO and controller boundary surface. Request bodies for analysis, game, and challenge flows now use bean validation consistently at the controller boundary. Remaining error-shape standardization work stays in `ARCH-18`.

Standard references:
- `docs/coding-standards.md`: "All external inputs must be validated and sanitized" and "Fail fast on invalid state; validate inputs at boundaries."
- `.github/instructions/api.instructions.md`: DTOs should validate request data at the boundary.

Historical evidence before `ARCH-17`:
- `WordGameController` and `ChallengeController` mixed bean validation with controller-local blank-request checks.
- `CreateChallengeRequest` and `ChallengeAssistRequest` lacked bean-validation constraints for optional-but-nonblank fields.
- The original April review also captured request DTO/controller gaps that had already started to move by the time this remediation landed.

Why correction is required:
- Validation behavior is inconsistent across the API surface.
- Invalid requests are handled ad hoc in controllers instead of by a consistent boundary contract.
- This increases the chance of internal exceptions, divergent error messages, and incomplete sanitization.

Required correction:
- Add Jakarta Bean Validation annotations to request DTOs in `com.fistraltech.server.dto`.
- Use `@Valid` on controller request bodies.
- Move basic request-shape validation out of controller bodies and into DTO constraints.

### P0. Exceptions are swallowed or reduced to non-actionable output

Current status (2026-04-25): resolved by `ARCH-18` for the active production failure paths. Session persistence failures now rethrow explicit operational errors, persisted-session reconstruction failures now surface as internal errors instead of collapsing to `null`/not-found, and the affected REST controllers now return one shared handled-error shape. Historical analyser console-output evidence from the April review was stale by the time this remediation landed and has been retained only as legacy context.

Standard references:
- `docs/coding-standards.md`: "Never swallow exceptions silently."
- `docs/coding-standards.md`: "Log only actionable information."
- `docs/coding-standards.md`: "Use structured logs."

Historical evidence before `ARCH-18`:
- `src/main/java/com/fistraltech/server/WordGameService.java` logged reconstruction failure and returned `null`, collapsing the error into an ambiguous "not found" outcome.
- `src/main/java/com/fistraltech/server/SessionPersistenceService.java` caught broad persistence exceptions, logged a warning, and continued after write failures.
- The original April review also cited analyser console-output and stack-trace paths that had already been cleaned up by the time the ticket was implemented.

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

Current status (2026-04-25): `ARCH-19` completed the first targeted high-risk coverage wave, and the later `ARCH-22` memoization work added focused regression coverage around shared entropy reuse. `SessionTrackingService`, `PlayerGameService`, and `CustomOAuth2UserService` now have focused regression coverage, the repository baseline later reached 72% overall line coverage, and the staged JaCoCo floor was ratcheted from 55% to 60%. The long-term 85% standard remains the target, but the highest-risk server/security blind spots named in the ticket are no longer effectively untested.

Standard reference:
- `docs/coding-standards.md`: "Minimum 85% line coverage."

Evidence:
- Current JaCoCo report at `target/site/jacoco/index.html` reports `71.99%` overall line coverage (`4,934 of 17,618` instructions missed; 494 of 1,254 branches missed).
- Targeted class gains from the same report include `SessionTrackingService` at 98% line coverage, `PlayerGameService` at 100%, and `CustomOAuth2UserService` at 97%.

Why correction is required:
- The codebase has a working suite, but it does not meet the agreed quality bar.
- The remaining gap is now narrower, but additional ratchets will still be needed to reach the long-term standard.

Required correction:
- Raise coverage toward the 85% minimum, prioritizing server flows, persistence failure paths, and untested utility/security behavior.
- Add regression tests for any bug fix work.

### P1. Error responses are inconsistent and do not meet the documented contract

Current status (2026-04-25): resolved by `ARCH-18` for the active server and security controller surface. Shared handled-failure mapping now lives behind `ApiErrors`, `ApiExceptionHandler`, and `SecurityApiExceptionHandler`, and the affected not-found, invalid-input, and internal-error flows now return the same `error` / `message` body shape.

Standard references:
- `docs/coding-standards.md`: "Error responses must follow a consistent schema."
- `docs/coding-standards.md`: structured errors should include an error code, human-readable message, and contextual metadata.
- `.github/instructions/api.instructions.md`: established API error responses use a stable map with `error` and `message`.

Historical evidence before `ARCH-18`:
- `AnalysisController`, `WordGameController`, `AuthController`, and `UserManagementController` used different ad hoc error maps and some empty-body 404/401 responses.
- `WordGameController` built not-found error bodies in some branches and then discarded them with `ResponseEntity.notFound().build()`.

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

Current status (2026-04-25): `ARCH-20` removed the residual legacy execution path from `WordEntropy`, so the class no longer carries dead matrix/cache-plumbing artifacts from the old design. Broader decomposition work for still-large classes remains tracked separately, especially for `WordGameController` and `WordGameService` under `ARCH-26`.

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

Current status (2026-04-25): resolved by `ARCH-27`. The remaining JUnit test classes now use the agreed `@DisplayName` convention consistently at class and test-method level, bringing CI and IDE output back in line with the repository testing instructions.

Standard references:
- `docs/coding-standards.md`: tests should be deterministic, isolated, and readable.
- `.github/instructions/testing.instructions.md`: class-level `@DisplayName` is always present.

Why correction is required:
- Tests are harder to scan in IDE and CI output.
- The suite is less uniform than the agreed test conventions require.

Required correction:
- Add class-level and test-level `@DisplayName` annotations where missing.
- Use the established naming format consistently across the remaining test classes.

### P3. Encapsulation is inconsistent in some domain types

Current status (2026-04-25): resolved by `ARCH-28`. `EntropyKey` now exposes only immutable `private final` state, validates required constructor arguments, and has focused regression coverage for constructor validation plus equality and ordering semantics.

Standard references:
- `docs/coding-standards.md`: "Encapsulation — hide internal details and expose only necessary interfaces."
- `docs/coding-standards.md`: "Avoid global mutable state" and implicit behavior.

Historical evidence before `ARCH-28`:
- `EntropyKey` previously exposed package-private mutable fields with equality and ordering logic based on mutable state, making it unsafe as a map/set key.

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