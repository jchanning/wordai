# ARCH-04 Remove Legacy Runtime Ownership

## Status

Completed on 2026-03-07.

## Goal

Retire the older non-Spring runtime path so the Spring service model is the only supported production path.

## Problem

- The codebase still contains older orchestration paths such as `DictionaryManager` and `game.GameController`.
- These overlap with the active Spring runtime model.
- Parallel runtime models create ambiguity around source of truth for config, dictionaries, and orchestration.

## Scope

- Audit usages of `DictionaryManager` and `game.GameController`.
- Remove them if unused, or deprecate briefly and then delete them.
- Ensure production code relies only on Spring-managed services.

## Acceptance Criteria

- `DictionaryManager` and `game.GameController` are either deleted or clearly deprecated and unused in production code.
- Spring services are the only active orchestration path for dictionaries and sessions.
- No production code path depends on singleton runtime orchestration.

## Notes

- Offline analysis support is not a target to preserve if unused.