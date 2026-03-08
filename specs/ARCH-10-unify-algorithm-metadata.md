# ARCH-10 Unify Algorithm Metadata

## Goal

Make algorithm metadata come from one source of truth.

## Problem

- Algorithm creation is registry-driven, but API exposure and some metadata are still hardcoded elsewhere.
- Adding a new algorithm currently risks drift across multiple classes.

## Scope

- Extend the registry or descriptor model to include display metadata.
- Generate the algorithms endpoint from the registry.
- Keep behavior for enabled and disabled algorithms consistent.

## Acceptance Criteria

- Algorithm id, display name, description, enabled state, and stateful flag come from one registry or catalog model.
- Adding a new algorithm does not require edits in multiple classes.
- The algorithms endpoint is generated from registry data rather than hardcoded lists.

## Status

Completed on 2026-03-07.

Algorithm descriptors now own id, display name, description, stateful behavior, and derived toggle-key metadata. The registry exposes that catalog, and the algorithms endpoint is generated from the feature service on top of the registry rather than hardcoded controller data.