# ARCH-01 Move Shared Dictionary Config Types

## Status

Completed on 2026-03-07.

## Goal

Move shared dictionary configuration types out of the HTTP layer so `core` and `util` no longer depend on `server.dto`.

## Problem

- `DictionaryOption` currently lives under `server.dto`.
- `core` and `util` import that type even though it is not an HTTP concern.
- This keeps the `core -> server` and `util -> server` architecture tests disabled.

## Scope

- Move `DictionaryOption` into a shared package owned by domain or configuration code.
- Update imports in configuration, dictionary loading, and controller code.
- Keep JSON response shape stable for API consumers.

## Acceptance Criteria

- `DictionaryOption` no longer lives under `server.dto`.
- `core` and `util` do not import from `server`.
- Dictionary loading and resolution behavior remains unchanged.
- The disabled `core -> server` and `util -> server` ArchUnit rules can be enabled and pass.

## Notes

- This is the first recommended refactor because it removes one of the most obvious layer leaks.