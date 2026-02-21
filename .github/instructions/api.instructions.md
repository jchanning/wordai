---
applyTo: "src/main/java/com/fistraltech/server/**/*.java"
---

# WordAI REST API Conventions

Apply these rules whenever adding or modifying controllers, services, or DTOs in the `server` package.

## Controller conventions
- Annotate with `@RestController` + `@RequestMapping("/api/wordai/<resource>")`.
- Use `@CrossOrigin` at the class level (already present on `WordGameController` — replicate the pattern).
- Constructor injection only — no `@Autowired` on fields.
- Return `ResponseEntity<T>` for all endpoints so HTTP status is explicit.
- Catch checked exceptions at the controller boundary and map to appropriate HTTP status:

| Exception | HTTP Status |
|---|---|
| `InvalidWordException` | `400 Bad Request` |
| `InvalidWordLengthException` | `400 Bad Request` |
| Session not found | `404 Not Found` |
| Unexpected `Exception` | `500 Internal Server Error` |

- Log using `java.util.logging.Logger` (already used throughout — do not switch to SLF4J).
- Include a Javadoc on the class listing the base path, resources, and a typical call flow (see `WordGameController` as the canonical example).

## Error response shape
All error responses must use this map structure (already established):
```java
Map<String, String> error = new HashMap<>();
error.put("error", "Short title");
error.put("message", "Human-readable detail");
return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
```

## DTO conventions
- One DTO per request body, one DTO per response body — never reuse request DTOs as responses.
- All fields private with public getters/setters (no Lombok — not on the classpath).
- Default no-arg constructor required for Jackson deserialization.
- Javadoc with a JSON example on every DTO class.
- Place in `com.fistraltech.server.dto`.

## Service conventions
- One `@Service` per resource domain (e.g. `WordGameService`, `DictionaryService`).
- Services hold game state in `ConcurrentHashMap` keyed by `String gameId` (UUID).
- Services are Spring-managed singletons — ensure thread-safety for shared state.
- Services must not contain HTTP concerns (no `HttpServletRequest`, no `ResponseEntity`).

## Layer rule reminder
`server` classes may import from `analysis`, `bot`, `core`, `util`.
Nothing outside `server` may import from `server`. Placing shared types (e.g. `DictionaryOption`) in `server.dto` and then importing them from `core` is a known violation — do not add more.

## OpenAPI / Swagger
The project uses `springdoc-openapi 2.6`. Swagger UI is available at `/swagger-ui.html`.
Annotate new endpoints with `@Operation(summary = "...")` and `@ApiResponse` if the response shape is non-obvious, but do not add annotations for simple CRUD endpoints — the auto-generated docs are sufficient.
