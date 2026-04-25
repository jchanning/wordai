# ARCH-18 Standardize Error and Failure Handling

## Status

Completed on 2026-04-25.

## Goal

Make API error responses and operational failure handling consistent, explicit, and diagnosable.

## Problem

- Error response shapes differ across controllers.
- Some code paths still swallow exceptions, downgrade failures to console output, or collapse distinct faults into ambiguous outcomes.
- This makes client behavior less predictable and production diagnosis harder.

## Scope

- Standardize the API error contract across server and security controllers.
- Centralize controller-level exception mapping where it reduces duplication.
- Replace `System.out`, `System.err`, and `printStackTrace()` usage in the targeted flows with structured logging.
- Tighten failure handling in persistence/reconstruction paths so faults are explicit.

## Acceptance Criteria

- Controllers return one consistent error shape for handled failures.
- Targeted production code no longer uses console output or stack-trace printing for operational failures.
- The affected flows distinguish not-found, invalid-input, and internal-failure cases clearly.
- Focused regression tests cover the changed error and failure paths.

## Notes

- Use the findings in `docs/coding-standards-findings.md` to drive the first slice.
- Coordinate with ARCH-17 when shared controller changes are involved.
