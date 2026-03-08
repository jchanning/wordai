# ARCH-06 Stop Controller Reach-Through

## Status

Completed on 2026-03-07.

## Goal

Stop controllers from reaching through one service to access another service's responsibilities.

## Problem

- Controllers currently call pass-through methods like `gameService.getDictionaryService()`.
- That couples controller code to internal wiring rather than resource ownership.
- It makes service boundaries weaker than they appear.

## Scope

- Inject resource-specific services directly into controllers.
- Remove service accessors that exist only to support controller reach-through.
- Preserve route paths and response behavior.

## Acceptance Criteria

- Controllers no longer call `gameService.getDictionaryService()` or similar pass-through accessors.
- Each controller depends directly on the service that owns its resource.
- Public routes stay unchanged.
- Controller tests cover the affected endpoints.