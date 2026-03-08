# ARCH-13 Refresh Architecture Documentation

## Goal

Update architecture documentation so it reflects the code after the refactor sequence.

## Problem

- Some documentation is now partly stale because parts of the older improvement list have already been implemented while other structural issues remain.
- Documentation should not be refreshed until code and tests match the intended design.

## Scope

- Update architecture docs after the structural refactors are complete.
- Remove stale improvement items that are already done.
- Document the single supported runtime path and enforced boundaries.

## Acceptance Criteria

- Architecture docs describe the actual runtime model, current boundaries, and removed legacy paths.
- Stale recommendations are deleted.
- The improvement doc reflects open work only, not already completed fixes.

## Notes

- Completed: architecture and implementation docs now describe the Spring-only runtime path, split resource controllers, registry-driven algorithm exposure, and resolved architecture-fitness debt.