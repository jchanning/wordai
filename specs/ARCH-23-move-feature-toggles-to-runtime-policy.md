# ARCH-23 Move Feature Toggles to Runtime Policy

## Status

Completed on 2026-04-25.

## Goal

Move operational feature toggles away from static application properties so algorithm exposure can be controlled without redeploys.

## Problem

- Current toggle behavior is property-driven, which ties operational changes to configuration redeploys.
- This limits incident response and controlled rollout options.
- The improvement backlog already identifies this as remaining design debt.

## Scope

- Define the runtime source of truth for feature toggles.
- Add the minimal persistence/service/admin surface needed to read and change toggle state safely.
- Keep algorithm exposure aligned with the registry-based model already in place.

## Acceptance Criteria

- Feature toggles can be changed through the chosen runtime policy without a redeploy.
- Algorithm exposure stays consistent with the registry and feature policy services.
- Operational and persistence behavior is documented.
- Tests cover the enabled/disabled behavior transitions.

## Notes

- Coordinate with existing admin/security patterns instead of introducing a parallel control path.
- Avoid expanding this ticket into a full experimentation platform.
