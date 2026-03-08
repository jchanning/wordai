# ARCH-12 Tighten Architecture Fitness Coverage

## Goal

Turn the documented architecture rules into enforced tests that block regressions.

## Problem

- Several architecture rules are currently disabled.
- Without automated enforcement, the intended layering remains advisory.

## Scope

- Enable disabled ArchUnit rules as supporting refactors land.
- Add or update tests for the single-runtime-model decision if needed.
- Keep test documentation aligned with actual package structure.

## Acceptance Criteria

- All intended layering tests are enabled in CI.
- New tests cover the single-runtime-model decision where useful.
- Documentation in the fitness test matches the actual package structure.
- A failing cross-layer import blocks merges.

## Notes

- This ticket should move in lockstep with ARCH-01, ARCH-02, and ARCH-03.
- Completed: all documented ArchUnit rules are now active, including the package cycle check and the server runtime-config centralisation rule.