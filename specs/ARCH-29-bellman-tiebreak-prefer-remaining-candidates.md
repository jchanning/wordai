# ARCH-29 Bellman Tie-Break: Prefer Potentially-Correct Candidates

## Status

Completed. Shipped in v1.15.10 (commit 8c2fe72).

## Goal

When `SelectBellmanFullDictionary` evaluates multiple words with equal expected remaining-dictionary reduction, prefer words still in the remaining (potentially correct) set over words known to be incorrect.

## Problem

- `SelectBellmanFullDictionary` scores every word in the full dictionary by expected remaining candidates after each guess. Two words can produce mathematically identical reduction scores (within floating-point tolerance).
- Before this change, tie resolution was determined by iteration order, which could select a known-incorrect word over a potentially correct one with no strategic benefit.
- This is suboptimal: if the game can be won this turn (the tie-breaking word is the actual answer), the old implementation would discard that opportunity.
- Observable consequence: the algorithm could waste a guess on a word it already knew was wrong, extending average solve length unnecessarily.

## Scope

- Add `isBetterGuess(double score, boolean guessCanBeCorrect, double bestScore, boolean bestWordCanBeCorrect)` private method to `SelectBellmanFullDictionary`.
- Track `bestWordCanBeCorrect` flag alongside `bestScore` in the main word-selection loop.
- Define `SCORE_TIE_EPSILON = 1e-9` to treat floating-point near-ties as equal.
- Three new unit tests in `SelectBellmanFullDictionaryTest`:
  - `prefersPotentiallyCorrectGuessWhenReductionTies()`
  - `evaluatesRemainingCandidatesWhenNoExternalGuessesAvailable()`
  - `neverSelectsSameWordTwice()`
- No change to public API or interface contracts.
- No change to non-tie-break scoring logic.

## Acceptance Criteria

- When two words produce equal expected reduction (within 1e-9), the word still in the remaining candidate set is preferred.
- When both tying words are in the remaining set, or neither is, the existing first-found winner is retained (no regression).
- All existing `SelectBellmanFullDictionary` tests continue to pass.
- New tests demonstrate the tie-break preference explicitly with a crafted two-candidate dictionary.

## Validation

```
mvn -Dtest=SelectBellmanFullDictionaryTest,SelectBellmanFullDictionaryRepeatedGuessTest test
```

All tests pass. `ArchitectureFitnessTest` and the full suite (`mvn clean verify`) also pass.

## Notes

- The change is confined to `bot.selection`; no server or core layer is modified.
- `SCORE_TIE_EPSILON` mirrors the tolerance already used in entropy comparison logic elsewhere in the selection package.
- This ticket was implemented ad-hoc (before the spec was written). Retrospective spec created to satisfy the spec-driven workflow requirement and maintain the governance record.
