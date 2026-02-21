---
description: 'Add a new REST endpoint to the WordAI API. Provide the HTTP method, path, and behaviour.'
---

Add a new REST endpoint to the WordAI API.

## Endpoint to implement
<!-- Fill in before running this prompt -->
**HTTP method:** [GET / POST / PUT / DELETE]
**Path:** `/api/wordai/[resource]`
**Purpose:** [What it does in one sentence]
**Request body (if any):** [Field names and types, or "none"]
**Response body:** [Field names and types]

## Checklist — follow all of these

### 1. Determine the correct controller
Check existing controllers in `com.fistraltech.server.controller`:
- Game session operations → `WordGameController`
- Analysis/bot runs → `AnalyticsController`
- User/admin → `AdminController` or `UserStatsController`
- If genuinely new domain → create a new `@RestController`

### 2. DTO
- Create a request DTO in `com.fistraltech.server.dto` if the endpoint takes a body.
- Create a response DTO if the response has more than 2 fields.
- Private fields, public getters/setters, no-arg constructor, Javadoc with JSON example.

### 3. Controller method
```java
@PostMapping("/path")
@Operation(summary = "Short description")
public ResponseEntity<?> methodName(@RequestBody RequestDto request) {
    try {
        // delegate to service
        return ResponseEntity.ok(result);
    } catch (InvalidWordException e) {
        Map<String, String> err = new HashMap<>();
        err.put("error", "Invalid word");
        err.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
    }
}
```

### 4. Service method
Add the business logic to the appropriate `@Service` class. Controllers must not implement business logic directly.

### 5. Test
Add at least two test cases to the relevant `*ControllerTest` using `@WebMvcTest` + `MockMvc`:
- Happy path: correct request → correct response + HTTP 200 (or 201).
- Error path: invalid input → HTTP 400 + error body.

### 6. Verify
```
mvn test -Dtest=[ControllerTest]
```

### Layer rule reminder
The new endpoint may use types from `analysis`, `bot`, `core`, `util`.
Do not put shared domain types in `server.dto` and then import them from `core` or `bot` — that creates a known violation pattern.
