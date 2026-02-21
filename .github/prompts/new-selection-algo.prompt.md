---
description: 'Implement a new SelectionAlgo strategy. Provide the algorithm name and selection logic description.'
---

Implement a new word selection strategy for WordAI.

## Algorithm to implement
<!-- Fill in before running this prompt -->
**Name:** [e.g. SelectVowelCoverage]
**Logic:** [e.g. Prefer words that cover the most unguessed vowels across positions]

## Requirements

1. **Class** in `com.fistraltech.bot.selection`, extending `SelectionAlgo`.
2. **Constructor** takes a `Dictionary` argument, calls `super(dictionary)`, and calls `setAlgoName("...")`.
3. **Override** `String selectWord(Response lastResponse, Dictionary dictionary)` — this is the only method to implement.
4. Use `DictionaryAnalytics` if the strategy needs letter frequency or entropy data.
5. If the dictionary is empty or has one word, return `dictionary.selectRandomWord()` as a safe fallback.
6. Do not construct `WordGame` inside a loop — if scoring requires simulating responses, use `ResponseMatrix` or `DictionaryAnalytics.getResponseBuckets(word)` which already cache appropriately.

## Reference implementation (SelectMaximumEntropy)
```java
public class SelectMaximumEntropy extends SelectionAlgo {
    public SelectMaximumEntropy(Dictionary dictionary) {
        super(dictionary);
        setAlgoName("MaximumEntropy");
    }

    @Override
    String selectWord(Response lastResponse, Dictionary dictionary) {
        return new DictionaryAnalytics(dictionary).getMaximumEntropyWord();
    }
}
```

## Test class required
Create `src/test/java/com/fistraltech/bot/selection/Select[Name]Test.java`:
- `@BeforeEach` builds a 10-word in-memory `Dictionary(5)` (use the words from `WordGamePlayerTest`).
- Test 1: first call returns a word from the dictionary.
- Test 2: after filtering down to 2 words the algo still returns a valid word.
- Test 3: `getAlgoName()` returns the expected name string.

## After creating the files
Register the strategy name in `AlgorithmFeatureService` so the UI and API can address it.
Run: `mvn test -Dtest=Select[Name]Test`
