# ARCH-09 Remove Placeholder Premium Analytics Endpoints

## Goal

Remove or hide unfinished premium analytics and export endpoints.

## Problem

- Premium analytics routes currently expose placeholder responses such as `Coming soon`.
- That advertises capability that does not exist.

## Scope

- Remove unfinished premium analytics/export endpoints, or hide them behind explicit feature gating.
- Align security rules with the reduced surface area.
- Leave implementation for a future ticket if the feature is still desired.

## Acceptance Criteria

- Unfinished premium analytics/export endpoints are removed, hidden, or replaced with real implementations.
- No production API advertises incomplete premium features.
- Security rules are updated to match the remaining surface area.

## Status

Completed on 2026-03-07.

The placeholder premium analytics controller was removed and security rules were updated so the old `/api/wordai/analytics/**` and `/api/wordai/export/**` route space no longer behaves like a live premium feature.