# Coding Standards

This document is the repository-wide coding standards reference for humans. The agent-facing enforcement rules live in [.github/instructions/](../.github/instructions/).

## Authority

When standards overlap, use this order:

1. active ticket or spec in [specs/](../specs/)
2. scoped instruction files in [.github/instructions/](../.github/instructions/)
3. [ARCHITECTURE.md](./ARCHITECTURE.md)
4. this document

## Java and Spring Boot

- Use descriptive names: camelCase for variables and methods, PascalCase for classes.
- Keep methods focused. If a method grows beyond a small, coherent unit, extract helpers instead of adding branches.
- Add Javadoc to public classes and public methods. Internal comments should explain intent, not restate the code.
- Prefer constructor injection for Spring-managed components. Do not use field injection.
- Do not introduce Lombok, records, or sealed classes casually in the server layer; follow the existing DTO style unless a ticket explicitly changes it.
- Use `java.util.logging.Logger` in server code. Do not introduce `System.out.println`, `System.err.println`, or `printStackTrace()` in production paths.

## Architectural Boundaries

- Keep `server` as the outermost layer. Nothing outside `server` should import from it.
- `DictionaryService` is the only supported runtime boundary for dictionary loading, dictionary option lookup, and shared entropy access from server code.
- Preserve the current layer model documented in [ARCHITECTURE.md](./ARCHITECTURE.md). Do not add cross-layer shortcuts to avoid writing a service or mapper.
- Preserve public API routes unless the active ticket explicitly changes them.

## REST API Rules

- Controllers use `@RestController`, `ResponseEntity<T>`, and constructor injection.
- Keep DTOs separate for request and response shapes.
- Use bean validation on API request DTOs and `@Valid` at controller boundaries where request bodies enter the system.
- For handled API failures, use the shared `ApiErrors` / controller-advice path instead of ad hoc per-controller maps, while preserving the established response shape:

```java
Map<String, String> error = new HashMap<>();
error.put("error", "Short title");
error.put("message", "Human-readable detail");
```

## Testing

- Use JUnit 5 for Java tests.
- Mirror the production package structure under `src/test/java`.
- Controller tests should use Spring test slices such as `@WebMvcTest` where practical.
- Pure unit tests should avoid filesystem, network, and system-state coupling.
- Prefer deterministic in-memory dictionaries in tests instead of loading dictionary files.
- The long-term coverage target remains 85% line coverage, but CI enforces a staged JaCoCo floor that ratchets upward from the current baseline instead of jumping there in one step.

## Validation Path

- Run the narrowest relevant validation first: `mvn -Dtest=ClassNameTest test`.
- Run `mvn clean verify` before declaring broader backend work complete.
- Run `npm run lint` when changing files under `src/main/resources/static/js`.
- Use `mvn verify -P security -DskipTests` for dependency-sensitive or security-profile work.
- The current staged CI floor enforces 60% bundle line coverage via JaCoCo during `mvn clean verify`.

## Configuration and Security

- Do not hardcode production secrets or credentials.
- Keep environment-specific behavior in configuration, profiles, or deployment scripts rather than buried in code branches.
- Fail loudly for missing required configuration instead of silently falling back to unsafe defaults in production paths.

## Documentation

- Update [IMPLEMENTATION_STATUS.md](./IMPLEMENTATION_STATUS.md) when a significant change lands.
- Update [README.md](../README.md) and [docs/README.md](./README.md) when the public entry points or authoritative governance docs change.
- Keep runtime/version references aligned with the live Maven and CI configuration.
