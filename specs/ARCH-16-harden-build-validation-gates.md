# ARCH-16 Harden Build Validation Gates

## Status

Completed on 2026-04-25.

## Goal

Strengthen automated validation so the repo enforces its documented quality bar instead of only reporting on it.

## Problem

- CI currently runs Maven verification, but the repo does not yet enforce a staged coverage threshold.
- Frontend linting exists in `package.json` but is not part of the main CI workflow.
- Security scanning is scheduled/manual rather than part of the normal merge path.
- The playbook calls for automated validation and fitness functions on every commit.

## Scope

- Add staged JaCoCo threshold enforcement in Maven.
- Add frontend lint execution to the main CI workflow.
- Expand security workflow triggers to cover pull requests or dependency-sensitive changes.
- Add one additional static-analysis gate only if it can be introduced without excessive noise.

## Acceptance Criteria

- The build fails when the current staged coverage threshold is missed.
- CI runs frontend lint alongside the existing Maven verification path.
- Security scanning runs on a merge-relevant trigger in addition to the weekly schedule.
- Any added static-analysis gate is documented and green in CI.

## Notes

- Use a staged ratchet, not an immediate jump to the long-term target.
- ARCH-18 should use the policy established here.
