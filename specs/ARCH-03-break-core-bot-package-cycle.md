# ARCH-03 Break Core Bot Package Cycle

## Status

Completed on 2026-03-07.

## Goal

Remove the cyclic dependency between `core` and `bot` so package structure matches the documented architecture.

## Problem

- `core` and `bot` currently form a cycle.
- The cycle is documented in the disabled architecture fitness test.
- Cycles make future refactors harder and blur ownership of filtering behavior.

## Scope

- Identify the minimum shared abstractions needed to decouple `core` from `bot`.
- Move filtering contracts or shared models to the correct layer.
- Keep `G`, `A`, `R`, and `X` response semantics unchanged.

## Acceptance Criteria

- The documented `core` and `bot` cycle is removed.
- The package-cycle ArchUnit rule is enabled and passes.
- Filtering behavior for `G/A/R/X` semantics is unchanged.
- Existing gameplay tests still pass.

## Notes

- This should be finished before deeper controller refactors, because cycles complicate package moves.
- Broader `analysis <-> bot` and `analysis <-> core` cycles still remain and are tracked separately under ARCH-12.