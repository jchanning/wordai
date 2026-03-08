# ARCH-05 Make DictionaryService Single Boundary

## Goal

Make `DictionaryService` the single runtime boundary for dictionary loading, cloning, and entropy access.

## Problem

- Dictionary responsibility is currently split across more than one abstraction.
- That makes it unclear which type owns preload behavior, cloning rules, and entropy caching.

## Scope

- Centralize dictionary runtime behavior in `DictionaryService`.
- Remove any duplicate lookup or preload paths outside this service.
- Keep startup preload and cached entropy behavior intact.

## Acceptance Criteria

- Dictionary loading, cloning, and entropy access come from one service boundary only.
- No controller or other service relies on a parallel dictionary manager.
- Startup still preloads configured dictionaries.
- Behavior for missing dictionaries is preserved.

## Status

Completed on 2026-03-07.

`DictionaryService` is now the only server-side runtime boundary for dictionary loading, cloning, and entropy access. The legacy parallel manager path was removed earlier, and the architecture fitness suite now blocks any other `server` class from depending on `ConfigManager` directly.