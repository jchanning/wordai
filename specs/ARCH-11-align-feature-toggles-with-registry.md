# ARCH-11 Align Feature Toggles With Registry

## Goal

Align algorithm feature-toggle configuration with the registry so configuration does not duplicate algorithm definitions manually.

## Problem

- Feature toggles are maintained separately from the algorithm registry.
- This creates duplication and the risk of drift.

## Scope

- Define a consistent mapping between registry descriptors and configuration keys.
- Apply the same toggle logic for execution and API exposure.
- Decide how unknown or misconfigured algorithms should fail.

## Acceptance Criteria

- Feature flags no longer duplicate algorithm definitions manually.
- Unknown algorithms cannot silently drift from configuration.
- Disabled algorithms are filtered consistently for execution and API exposure.

## Status

Completed on 2026-03-07.

Feature-toggle lookup now derives property names from descriptor metadata through the registry, so algorithm definitions are no longer manually duplicated in `AlgorithmFeatureService`. Execution validation and API exposure both use the same registry-backed toggle resolution.