# ARCH-28 Tighten EntropyKey Encapsulation

## Status

Completed on 2026-04-25.

## Goal

Make `EntropyKey` immutable and explicit so it is safe to use in maps, sets, and ordering logic.

## Problem

- [docs/coding-standards-findings.md](../docs/coding-standards-findings.md) still identifies `EntropyKey` as exposing mutable package-private state.
- Mutable equality and ordering keys are easy to misuse and can cause subtle collection bugs.

## Scope

- Make `EntropyKey` fields private and final.
- Add explicit construction and validation for required values.
- Preserve existing equality, hashing, and ordering semantics on immutable state.

## Acceptance Criteria

- The remaining encapsulation finding is represented in the active backlog.
- `EntropyKey` exposes immutable state only.
- Focused tests cover the construction and comparison behavior if the current suite does not already do so.

## Validation

- `mvn "-Dtest=EntropyKeyTest" test --no-transfer-progress -q`

## Notes

- Derived from the P3 encapsulation finding in [docs/coding-standards-findings.md](../docs/coding-standards-findings.md).
- The production `EntropyKey` implementation already satisfied the immutability requirements when this ticket was executed; the remaining work was focused regression coverage and backlog/status alignment.