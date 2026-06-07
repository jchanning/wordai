# ARCH-30 Add Manual Wordle Feedback Adapter

## Status

Completed on 2026-06-06.

## Goal

Let the core model accept external Wordle feedback and turn it into the same response shape that the existing filter and suggestion logic already understand.

## Problem

- `ResponseHelper.evaluate()` only works when the project knows the hidden target word.
- The manual-assistant feature needs to consume guess + feedback from the New York Times Wordle game, where the target word is unknown to WordAI.
- Today there is no core-level adapter that converts user-entered feedback into a reusable `Response` object.

## Scope

- Add a core-level feedback adapter or factory that builds a `Response` from a guessed word plus external color feedback.
- Normalize the imported feedback into the existing `G`, `A`, `R`, and `X` semantics used by `Filter`.
- Validate word length and feedback length before the response is accepted.
- Preserve the existing evaluation path for normal in-app games; do not change `ResponseHelper.evaluate()` behavior.
- Add core unit tests for the adapter, including duplicate-letter and invalid-feedback cases.

## Acceptance Criteria

- A manual Wordle turn can be represented as a `Response` without a target word.
- Invalid feedback length or unsupported feedback codes fail fast with a clear exception.
- Duplicate-letter cases map to the same filtering semantics as the current evaluator.
- Existing automated game evaluation continues to pass unchanged.

## Validation

- Focused core tests for the new adapter plus the existing WordGame and response-pattern tests.

## Notes

- This ticket is the core foundation for the assistant feature; server and UI work should build on top of the imported `Response` shape instead of duplicating feedback logic.