# ARCH-31 Introduce Manual Assistant Session API

## Status

Completed on 2026-06-06.

## Goal

Add a server-side assistant session flow that stores the current candidate set, accepts manual Wordle feedback, and returns the next recommended guess.

## Problem

- The current server API is centered on `GameSession`, which always assumes a hidden target word and a live in-app game.
- Manual Wordle assistance needs a target-free flow where the user provides the guess and the feedback from the external game.
- The current suggestion endpoint is tied to live game sessions and does not expose a dedicated assistant workflow.

## Scope

- Add an assistant-session model or service that keeps the current filtered dictionary, selected strategy, and manual-feedback history.
- Add request/response DTOs for starting an assistant session, submitting feedback, and requesting the next recommendation.
- Expose assistant endpoints under the versioned API root so the manual assistant flow is separate from standard game play.
- Reuse the existing selection algorithms and filter logic instead of introducing a new recommendation engine.
- Keep the normal `/games` endpoints unchanged.

## Acceptance Criteria

- A client can create an assistant session without choosing a target word.
- A client can submit a guessed word plus external feedback and have the candidate dictionary shrink accordingly.
- A client can request the next suggestion from the current assistant state.
- The assistant flow does not mutate or depend on the standard `WordGame` target-word lifecycle.

## Validation

- Focused server service and controller tests for the assistant-session lifecycle and recommendation output.

## Notes

- This ticket should remain isolated from the normal game session code path so the existing game API stays stable.