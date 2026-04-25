# ARCH-14 Establish Governance Source of Truth

## Status

Completed on 2026-04-25.

## Goal

Consolidate the project's design and governance documents into one coherent, trustworthy source of truth.

## Problem

- The repo already has strong architecture and feature specs, but the governance set is fragmented across `docs/`, `specs/`, and `.github/instructions/`.
- Several playbook artifacts are missing or incomplete, including a populated coding standards document, glossary, state-machine documentation, and a dedicated API sketch.
- Some written platform/version references are stale relative to the live Maven and CI configuration.

## Scope

- Create or refresh the missing governance artifacts needed by the Vibe Coding Playbook.
- Populate `docs/coding-standards.md` and define which documents are authoritative.
- Reconcile stale stack/version references across `README.md`, `docs/README.md`, and architecture/development docs.
- Cross-link the governance docs from the docs index and top-level README.

## Acceptance Criteria

- The governance set includes a Blueprint/Master Plan equivalent, an execution playbook, an API sketch, a glossary, and state-machine documentation.
- `docs/coding-standards.md` is populated and aligned with the current repo instructions.
- Public documentation describes the actual build/runtime baseline in `pom.xml` and CI.
- The docs index and top-level README point to the authoritative governance documents.

## Notes

- This is the foundation ticket for the remediation wave.
- ARCH-15 through ARCH-19 should build on the document set established here.
