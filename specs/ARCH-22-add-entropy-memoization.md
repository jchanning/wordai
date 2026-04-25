# ARCH-22 Add Entropy Memoization

## Status

Completed on 2026-04-25.

## Goal

Avoid redundant entropy recomputation across repeated equivalent filter states.

## Problem

- `docs/ARCHITECTURE_IMPROVEMENTS.md` calls out repeated entropy recomputation with no memoization.
- Different sessions can reach identical effective filter states and still pay the full entropy cost repeatedly.
- This leaves avoidable performance work in a hot analysis/selection path.

## Scope

- Define a canonical memoization key for the relevant filter state.
- Add bounded caching for entropy results where it provides measurable reuse.
- Preserve correctness and thread-safety expectations for the existing selection flows.

## Acceptance Criteria

- Equivalent filter states can reuse prior entropy computations.
- Memoization is bounded and does not introduce uncontrolled memory growth.
- Selection behavior remains correct for the supported algorithms.
- Performance/documentation notes describe the new caching behavior.

## Notes

- Keep this ticket focused on reuse of computed entropy, not on broader algorithm redesign.
- Revisit the cache policy only after correctness and observability are in place.
