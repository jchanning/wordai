# ARCH-32 Add Manual Assistant UI Workflow

## Status

Completed on 2026-06-06.

## Goal

Expose the manual Wordle assistant in the existing UI so users can enter the guess they made in NYT Wordle, enter the color feedback they received, and see the next recommended guess.

## Problem

- The current frontend supports normal in-app guesses and a simple suggestion fetch, but it does not let a user enter external Wordle feedback.
- Without a dedicated input flow, users have no supported path from the assistant panel into the new manual-assistance API.

## Scope

- Extend the existing assistant panel or adjacent UI area with inputs for the guessed word and its feedback pattern.
- Add client-side validation for the feedback format before the request is sent.
- Wire the UI to the new assistant-session endpoints and render the returned recommendation.
- Preserve the normal game board, autoplay, and analysis modes.
- Show clear inline errors when the manual feedback is invalid or the assistant session rejects the update.

## Acceptance Criteria

- A user can enter a Wordle guess and the color-coded feedback from the NYT game and receive a next-guess recommendation.
- The UI keeps the assistant state visible across multiple manual turns.
- Normal game play remains unchanged.
- API or validation errors are surfaced in a way that tells the user how to correct the input.

## Validation

- `npm run lint`
- Manual browser smoke check of the assistant flow in the running application

## Notes

- Prefer reusing the current assistant panel vocabulary so the new workflow feels like an extension of the existing suggestion feature rather than a separate tool.