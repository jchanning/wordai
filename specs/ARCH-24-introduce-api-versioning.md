# ARCH-24 Introduce API Versioning

## Status

Completed on 2026-04-25.

## Goal

Introduce an explicit API version boundary before more external contract changes accumulate.

## Problem

- The current API has no version prefix.
- Any future breaking change would force every client onto the new contract at once.
- The architecture backlog already identifies versioning as unresolved design debt.

## Scope

- Define the versioning approach for the current REST API.
- Introduce the initial version boundary without regressing current supported clients unnecessarily.
- Update API documentation and routing tests to reflect the versioned surface.

## Acceptance Criteria

- The API has an explicit version boundary for supported routes.
- Routing and documentation reflect the versioned contract.
- Backward-compatibility decisions are documented.
- Tests cover the versioned routing behavior.

## Notes

- Decide early whether this is a clean prefix migration or a compatibility bridge.
- Keep the first versioning step small and explicit.
