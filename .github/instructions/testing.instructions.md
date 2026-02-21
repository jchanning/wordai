---
applyTo: "src/test/**/*.java"
---

# WordAI Test Conventions

These rules apply to every test file. Follow them exactly — do not deviate without a stated reason.

## Framework
- JUnit 5 (`org.junit.jupiter`). No JUnit 4.
- Spring Boot test slice annotations (`@WebMvcTest`, `@DataJpaTest`) for controller/repository tests.
- Mockito via `spring-boot-starter-test` for mocking — prefer `@MockBean` in Spring slice tests, `Mockito.mock()` in pure unit tests.

## Naming and structure
```java
@DisplayName("ClassName Tests")          // class level — always present
class ClassNameTest {

    @BeforeEach
    void setUp() { ... }

    @Test
    @DisplayName("methodName_condition_expectedBehaviour")
    void methodName_condition_expectedBehaviour() { ... }

    @Nested
    @DisplayName("When <scenario>")      // group >5 related tests
    class WhenScenario {
        @Test
        @DisplayName("should ...")
        void should...() { ... }
    }
}
```

## Dictionary setup — unit tests
Never load from files. Build a small, deterministic in-memory dictionary:
```java
@BeforeEach
void setUp() {
    dictionary = new Dictionary(5);
    Set<String> words = new HashSet<>();
    words.add("arose"); words.add("stare"); words.add("crane");
    words.add("slate"); words.add("raise");
    dictionary.addWords(words);
}
```
Minimum 5 words so `SelectionAlgo` and `Filter` have something meaningful to operate on.

## Config setup — unit tests
```java
Config config = new Config();
config.setMaxAttempts(20);   // generous for small-dictionary tests
```

## Assertions
- `assertEquals(expected, actual)` — always expected first.
- Prefer specific assertions over `assertTrue(x.equals(y))`.
- For collections: `assertFalse(list.isEmpty())` before accessing elements.
- Use `@RepeatedTest(n)` for randomised behaviour (e.g. `SelectRandom`) — 5–10 repetitions.

## Package placement
Mirror the production package under `src/test/java/com/fistraltech/`:
| Production class | Test class |
|---|---|
| `com.fistraltech.bot.WordGamePlayer` | `com.fistraltech.bot.WordGamePlayerTest` |
| `com.fistraltech.bot.selection.SelectFoo` | `com.fistraltech.bot.selection.SelectFooTest` |
| `com.fistraltech.analysis.DictionaryAnalytics` | `com.fistraltech.analysis.DictionaryAnalyticsTest` |

## What makes a complete test class
1. At least one test per public method.
2. A test for the happy path.
3. A test for a meaningful edge case (empty dictionary, single word, `null` response).
4. No tests that depend on external files, network, or system state.

## Running tests
```
mvn test -Dtest=ClassNameTest           # one class
mvn test -Dtest="ClassNameTest#method"  # one method
mvn clean test                          # full suite
```

All tests must pass before declaring a task complete.
