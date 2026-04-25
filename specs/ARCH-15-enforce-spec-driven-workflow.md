# ARCH-15 Enforce Spec-Driven Workflow

## Status

Completed on 2026-04-25.

## Goal

Turn the documented governance model into a repeatable workflow for day-to-day development.

## Problem

- The repo contains feature specs, architecture tickets, and status tracking, but the workflow for when each artifact is required is not yet explicit.
- Known remediation work still lives partly in narrative docs instead of the active ticket backlog.
- There is no shared template or lightweight commit-time workflow to keep specs, tests, and status updates moving together.

## Scope

- Add or document the template used for new feature/remediation tickets in `specs/`.
- Normalize current open remediation work into the ticket backlog.
- Define the required workflow for significant changes: spec link, tests, and `docs/IMPLEMENTATION_STATUS.md` update.
- Document or scaffold a fast local validation path for pre-commit use.

## Acceptance Criteria

- New work in `specs/` follows one documented template.
- Open remediation work from the current findings docs is represented in the active ticket backlog.
- Contribution/development guidance states the minimum workflow for spec, test, and status updates.
- Developers have a documented fast local check path for the agreed narrow validations.

## Notes

- Depends on ARCH-14 defining the authoritative governance documents.
- This ticket is about workflow enforcement, not about fixing the underlying code issues.
