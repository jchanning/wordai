# ARCH-08 Remove Placeholder Stats Endpoints

## Goal

Remove or hide placeholder stats endpoints until they have real implementations.

## Problem

- Some authenticated stats endpoints return fake or zeroed values.
- Placeholder production routes increase maintenance surface and confuse clients.

## Scope

- Remove unfinished user stats routes from public exposure, or explicitly disable them.
- Keep only routes backed by real persisted data.
- Update API docs accordingly.

## Acceptance Criteria

- Stub endpoints that return placeholder values are either removed from routing or explicitly disabled from public use.
- Swagger/OpenAPI no longer advertises unfinished features.
- No controller returns fake production stats data.

## Status

Completed on 2026-03-07.

The placeholder authenticated stats controller was removed. The old `/api/wordai/stats/**` route space is no longer mapped and now resolves as missing instead of returning fake data.