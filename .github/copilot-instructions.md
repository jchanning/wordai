# WordAI Copilot Instructions (Updated)

WordAI is a Wordle-like simulation and analysis system. This guide equips AI agents to make correct, fast changes aligned with project conventions.

## Architecture Map
- `com.fistraltech.core`: Game primitives
	- `WordGame`: Applies a guess to a target; produces `Response` of per-position codes: `G` (green), `A` (amber), `R` (red), `X` (excess).
	- `Dictionary`: Word set, word length, column stats (`Column`), and helpers.
- `com.fistraltech.bot`: Bots and game orchestration
	- `WordGamePlayer` + `SelectionAlgo` strategies (e.g., random, letter-frequency, fixed-first-word).
	- `filter.Filter`: Prunes dictionary using `Response` and occurrence rules.
- `com.fistraltech.analysis`: Analytics
	- `DictionaryAnalytics`: letter/position stats, response buckets, entropy and max-entropy word.
	- `GameAnalytics`: writes CSV summaries; `ComplexityAnalyser`: performance.
- `com.fistraltech.server`: Simple HTTP API and static UI (`static/css`, `static/js`, `index.html`).

## Core Patterns (must follow)
- Response rules:
	- `G`: fix letter at position; exclude others there.
	- `A`: letter present elsewhere; remove from guessed position; add to must-contain.
	- `X`: letter present but too many occurrences; treat like `A` and track counts.
	- `R`: letter absent; remove from all positions.
- Selection strategies implement `SelectionAlgo.selectWord(Response lastResponse, Dictionary dictionary)`.
- Entropy: `DictionaryAnalytics.getEntropy(word)` computes $-\sum p \log_2 p$ from `getResponseBuckets(word)`.

## Build, Test, Run
- Build/tests: `mvn clean test` (JUnit 5). Artifacts in `target/`.
- Run server/UI (if present): check `WordAIApplication` main and `server/*` controllers.
- Static UI split: logic in `src/main/resources/static/js/game.js`, styles in `static/css/style.css`.

## Configuration
- Defaults: `src/main/resources/application.properties`.
- Optional override: `wordai.properties` at repo root; `ConfigManager` resolves paths.
- Dictionary fallback: `src/main/resources/dictionaries/5_letter_words.txt` if external path missing.

## Conventions & Gotchas
- Word length is fixed by config; validate indices when iterating positions to avoid `ArrayIndexOutOfBounds`.
- Keep classes in their packages; avoid leaking server concerns into core/bot/analysis.
- When computing buckets/entropy, reuse a single `WordGame` if possible; current code constructs per target—cache only if performance becomes an issue.
- Tests live under `src/test/java/com/fistraltech/...`; mimic existing naming and package layout.

## Examples
- Implement a new selection algo:
	```java
	public class SelectHighestEntropy extends SelectionAlgo {
			@Override
			public String selectWord(Response lastResponse, Dictionary dictionary) {
					return new DictionaryAnalytics(dictionary).getMaximumEntropyWord();
			}
	}
	```
- Use response buckets:
	```java
	Map<String, Set<String>> buckets = analyser.getResponseBuckets("slate");
	// keys like "GARXR" denote per-position feedback
	```

## Analytics Outputs
- CSVs under `${user.home}/OneDrive/Projects/Wordlex`: `summary-*.csv`, `details-*.csv`, `columns-*.csv`.
- Use `GameAnalytics` to append per-game metrics.

## CI/Editor Tips
- VS Code: Java extensions, `mvn clean compile` via terminal.
- Prefer incremental, surgical edits; keep APIs stable unless explicitly changing strategy.

If any section is unclear (e.g., server routes, Filter character accounting), tell us and we’ll refine this guide.
