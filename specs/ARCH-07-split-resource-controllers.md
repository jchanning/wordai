# ARCH-07 Split Resource Controllers

## Goal

Split controller responsibilities by resource domain while keeping existing API routes stable.

## Problem

- The main game controller still mixes game lifecycle, dictionary catalog, analysis, algorithms, and history concerns.
- That increases change risk and obscures ownership.

## Scope

- Keep game-session lifecycle endpoints in a game-focused controller.
- Move dictionary catalog/read endpoints into a dictionary-focused controller.
- Move history or other non-game resources into their own controllers as appropriate.

## Acceptance Criteria

- Game session lifecycle endpoints are isolated from dictionary catalog and history endpoints.
- Route paths remain backward-compatible.
- Each controller has a single clear resource domain.
- Existing API behavior is preserved.

## Status

Completed on 2026-03-07.

Implemented by moving dictionary, algorithm, analysis, and history routes into dedicated controllers while leaving game-session lifecycle routes in `WordGameController`. Public paths were preserved.