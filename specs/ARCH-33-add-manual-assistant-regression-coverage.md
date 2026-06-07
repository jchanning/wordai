# ARCH-33 Add Manual Assistant Regression Coverage

## Status

Completed on 2026-06-06.

## Goal

Add regression coverage for the manual-assistant feature so the new feedback-import path, server recommendation flow, and UI contract remain stable while the feature is implemented.

## Problem

- The new workflow spans multiple layers and is easy to break if only one layer is tested in isolation.
- Duplicate letters, invalid color patterns, and filter updates are the most likely places for subtle regressions.
- The current test suite does not yet cover a target-free assistant flow.

## Scope

- Add focused core tests for feedback parsing and duplicate-letter semantics.
- Add server tests that verify the assistant session updates the candidate dictionary and returns a recommendation.
- Add frontend-facing regression coverage where practical, or document a manual smoke check if an automated browser test is not available yet.
- Update API and user-facing documentation to describe the manual Wordle assistant contract and feedback format.

## Acceptance Criteria

- The new manual-assistant path has automated coverage for invalid input and a successful recommendation path.
- Duplicate-letter feedback behaves consistently with the existing WordAI filtering rules.
- The API documentation describes how the assistant feedback should be entered and what the server returns.

## Validation

- Focused test runs for the new core and server coverage, followed by a full `mvn test` pass once the feature slice is wired together.

## Notes

- This is the final phase-1 safety net; it should be kept aligned with the core adapter and assistant-session API so regressions are caught at the boundary closest to the user input.