# ARCH-17 Fix API Boundary Validation

## Status

Completed on 2026-04-25.

## Goal

Make API boundary validation consistent so invalid external input is rejected before it reaches core service logic.

## Problem

- Validation is applied inconsistently across the server and security controllers.
- Several DTOs lack bean validation annotations, forcing controller methods to perform ad hoc checks.
- This weakens fail-fast behavior and increases the chance of inconsistent request handling.

## Scope

- Add bean validation annotations to API-facing request DTOs where missing.
- Apply `@Valid` at controller boundaries where requests enter the system.
- Remove controller-local request-shape checks that should be handled by DTO validation.
- Add focused controller/service tests for invalid-request paths.

## Acceptance Criteria

- API request DTOs enforce their basic invariants through bean validation.
- Controllers reject invalid request bodies through one consistent validation path.
- Manual request-shape validation in controllers is reduced to business rules that cannot live in DTO constraints.
- Focused tests cover the main invalid-input cases for the changed endpoints.

## Notes

- Use `docs/coding-standards-findings.md` as the initial defect list.
- This ticket should land before broader error-handling cleanup where practical.
