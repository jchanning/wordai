# ARCH-20 Remove WordEntropy Legacy Paths

## Status

Completed on 2026-04-25.

## Goal

Remove the deprecated and partially dead legacy execution path in `WordEntropy` so the class reflects the current matrix-based design only.

## Problem

- `docs/deprecated-code-removal-plan.md` documents a dual-path design in `WordEntropy` where the legacy cache-based path was never fully removed.
- Dead branches, deprecated helpers, and orphan support types make the class harder to reason about and maintain.
- This drift obscures the true performance architecture described in the docs.

## Scope

- Execute the safe dead-code and migration steps from the deprecated-code removal plan.
- Remove legacy cache-path support once matrix-based behavior is the only live path.
- Delete orphan support code and tests after their callers are gone.

## Acceptance Criteria

- `WordEntropy` no longer carries dead `useMatrix` branching or legacy cache-path plumbing.
- Any removed support classes/tests are either deleted or replaced with matrix-aligned coverage.
- The class matches the architecture and performance documentation.
- The relevant analysis/core tests pass after the cleanup.

## Notes

- Follow the execution order documented in `docs/deprecated-code-removal-plan.md`.
- Keep this work separate from broader coverage-gate changes.
