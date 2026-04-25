# ARCH-19 Raise High-Risk Server Coverage

## Status

Completed on 2026-04-25.

## Goal

Raise automated test coverage where the current risk is highest instead of chasing broad percentage gains first.

## Problem

- Several server and security classes with sensitive behavior have extremely low coverage.
- The current overall coverage baseline is too low to support an immediate hard jump to the desired long-term threshold.
- Without targeted regression tests, future cleanup work in those areas remains high risk.

## Scope

- Add focused tests for the lowest-covered high-risk classes first.
- Prioritize controller, session-tracking, persistence/recovery, and security-service paths.
- Align package/class targets with the staged coverage policy introduced by ARCH-16.
- Keep `docs/IMPLEMENTATION_STATUS.md` and any coverage guidance in sync with the real baseline.

## Acceptance Criteria

- The targeted high-risk classes gain meaningful regression coverage.
- Package/class coverage moves upward from the current baseline in a measurable way.
- The build remains aligned with the staged threshold policy from ARCH-16.
- Documentation reflects the new baseline and next ratchet target.

## Notes

- Prioritize `AdminController`, `AuthController`, `SessionTrackingService`, `CustomOAuth2UserService`, `PlayerGameService`, and adjacent `WordGameService` flows.
- This ticket should follow the validation-gate policy set in ARCH-16.
