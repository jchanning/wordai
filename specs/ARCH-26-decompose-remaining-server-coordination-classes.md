# ARCH-26 Decompose Remaining Server Coordination Classes

## Status

Completed on 2026-04-25.

## Goal

Reduce the remaining oversized server coordination classes so controller and service behavior is easier to reason about and test.

## Problem

- [docs/coding-standards-findings.md](../docs/coding-standards-findings.md) still identifies `WordGameController` and `WordGameService` as large, multi-responsibility types.
- Existing tickets already cover `WordEntropy` and `GameSession`, but they do not address the remaining controller/service orchestration debt.
- Leaving these classes oversized increases review cost and makes later behavior changes riskier.

## Scope

- Split controller assembly and response-shaping responsibilities from core orchestration paths where it improves clarity without changing public routes.
- Extract smaller collaborators or helpers from `WordGameController` and `WordGameService` where the seams are already present.
- Keep the public REST contract stable unless a separate ticket explicitly changes it.

## Acceptance Criteria

- The remaining oversized server coordination work is tracked by one explicit backlog ticket.
- `WordGameController` and `WordGameService` lose at least one distinct responsibility each through extracted collaborators or helpers.
- Focused regression tests cover the changed orchestration seams.

## Validation

- `mvn -Dtest=ClassNameTest test` for the touched server slice.
- `mvn clean verify` if the refactor crosses multiple backend packages.

## Notes

- Derived from the P2 size and separation-of-concerns findings in [docs/coding-standards-findings.md](../docs/coding-standards-findings.md).
- Keep this work separate from API contract changes and broader error-handling cleanup.