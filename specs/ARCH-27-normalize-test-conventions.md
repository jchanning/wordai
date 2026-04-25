# ARCH-27 Normalize Test Conventions

## Status

Completed on 2026-04-25.

## Goal

Make the remaining JUnit test classes follow one consistent readability convention.

## Problem

- [docs/coding-standards-findings.md](../docs/coding-standards-findings.md) still identifies test classes without the agreed `@DisplayName` usage.
- Inconsistent test presentation makes CI output harder to scan and weakens the repository’s testing conventions.

## Scope

- Add the missing class-level and test-level `@DisplayName` annotations where the test convention requires them.
- Normalize the touched test classes to the current project naming style.
- Keep the ticket limited to presentation and convention cleanup, not behavior changes.

## Acceptance Criteria

- The remaining test-convention cleanup is represented in the active backlog.
- The targeted tests use the agreed `@DisplayName` convention consistently.
- The touched tests still pass without changing production behavior.

## Validation

- `mvn -Dtest=ClassNameTest test` for each touched test class or focused group.

## Notes

- Derived from the P3 testing-convention findings in [docs/coding-standards-findings.md](../docs/coding-standards-findings.md).