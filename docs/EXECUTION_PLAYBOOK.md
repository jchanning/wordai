# Execution Playbook

This document is the day-to-day workflow source of truth for WordAI changes. It turns the architecture backlog and repository instructions into one repeatable delivery path.

## Scope

Use this playbook for:
- architecture remediation tickets in [specs/](../specs/)
- feature work that changes production behavior
- test additions tied to a tracked change
- documentation updates that establish or change repository policy

## Governance Order

Apply documents in this order when they overlap:

| Priority | Source | Purpose |
|---|---|---|
| 1 | [specs/README.md](../specs/README.md) and the active ticket in [specs/](../specs/) | Change intent, scope, acceptance criteria |
| 2 | [.github/instructions/copilot-instructions.md](../.github/instructions/copilot-instructions.md) plus scoped instruction files | Repository coding, API, UI, and test rules |
| 3 | [ARCHITECTURE.md](./ARCHITECTURE.md) | Blueprint and master-plan equivalent |
| 4 | [coding-standards.md](./coding-standards.md) | Human-readable implementation standards |
| 5 | [IMPLEMENTATION_STATUS.md](./IMPLEMENTATION_STATUS.md) | Current completion state and follow-up work |

If two sources disagree, the higher-priority source wins and the lower-priority document should be updated.

## Required Workflow

### 1. Start From a Ticket or Spec

- Link the work to one ticket in [specs/](../specs/).
- Use [specs/TEMPLATE.md](../specs/TEMPLATE.md) for new remediation or governance tickets.
- If the work is not small enough to explain in one paragraph, create or update a spec before editing code.
- Preserve existing public routes unless the ticket explicitly changes them.

### Required Artifacts for Significant Changes

For any change that alters production behavior, architecture, validation policy, or repository workflow:

- keep one active ticket in [specs/](../specs/)
- add or update focused tests for the changed behavior
- update [IMPLEMENTATION_STATUS.md](./IMPLEMENTATION_STATUS.md)
- update human-readable docs when API shape, workflow, or architecture guidance changes

### 2. Gather Only the Local Context You Need

- Read the owning implementation surface, its immediate tests, and the active ticket.
- Prefer one falsifiable local hypothesis over broad repo exploration.
- If the issue is in the server layer, validate controller, DTO, and service boundaries before changing behavior.

### 3. Make the Smallest Coherent Change

- Keep edits scoped to one acceptance slice at a time.
- Do not mix unrelated cleanup into the same change.
- Update documentation when the change alters architecture, workflow, public API shape, or the verified runtime baseline.

### 4. Validate Immediately

Run the narrowest check that can fail the current hypothesis:

| Change type | First validation |
|---|---|
| One Java test slice | `mvn -Dtest=ClassNameTest test` |
| One controller test slice | `mvn -Dtest=ClassNameTest test` |
| Broader backend change | `mvn clean verify` |
| Static UI / frontend JS change | `npm run lint` |
| Dependency or security-sensitive change | `mvn verify -P security -DskipTests` |
| Documentation-only change | Re-read the touched docs and confirm links, names, and baselines |

### 5. Update Status Artifacts

For every significant change:
- update [IMPLEMENTATION_STATUS.md](./IMPLEMENTATION_STATUS.md)
- keep the active ticket status in [specs/](../specs/) honest
- add or refresh links from [README.md](../README.md) and [docs/README.md](./README.md) when governance documents change

### 6. Prepare the Commit / PR Slice

- Keep one acceptance slice per branch or PR.
- Name the active ticket and the focused validation path in the change summary.
- Do not treat the change as done until the ticket, status doc, and validation evidence agree.

## Fast Local Paths

Use the narrow path first, then the broader gate if the slice expands.

```bash
mvn -Dtest=ClassNameTest test
mvn clean verify
npm run lint
```

## Done Criteria

The change is not complete until all of the following are true:
- the active ticket acceptance slice is implemented
- the narrowest relevant validation passes
- docs are updated if the source of truth changed
- [IMPLEMENTATION_STATUS.md](./IMPLEMENTATION_STATUS.md) reflects the new state
- the active ticket status is updated to match reality

## Current Repository Baseline

- Local development and CI compile on Java 25.
- The cloud Maven profile targets Java 17.
- The current Dockerfile remains pinned to Temurin 21 and should be treated as a separate container baseline until aligned.
- Mainline CI runs `mvn clean verify`.
- Mainline CI also runs `npm run lint`, and Maven now enforces a staged JaCoCo bundle line floor of 60% during `verify`.