# Contributing to WordAI

This repository follows a spec-driven workflow for significant changes. Use this guide for the minimum contributor path; use [docs/EXECUTION_PLAYBOOK.md](./docs/EXECUTION_PLAYBOOK.md) for the full day-to-day execution rules.

## Start with a ticket

- Link every significant change to one ticket in [specs/](./specs/).
- Create new tickets from [specs/TEMPLATE.md](./specs/TEMPLATE.md).
- Keep one ticket focused on one acceptance slice. Do not mix unrelated cleanup into the same branch.

## Required artifacts for significant changes

For any change that alters production behavior, architecture, validation policy, or repository workflow:

- update or create the active ticket in [specs/](./specs/)
- add or update focused tests for the changed behavior
- update [docs/IMPLEMENTATION_STATUS.md](./docs/IMPLEMENTATION_STATUS.md)
- update human-readable docs when API shape, workflow, or architecture guidance changes

## Local workflow

1. Read the active ticket and the owning implementation surface.
2. Form one local hypothesis and make the smallest coherent change that can test it.
3. Run the narrowest relevant validation immediately.
4. Expand to broader validation only when the slice grows.
5. Update status and documentation before considering the work complete.

## Fast local validation path

Use the cheapest check that can fail the current slice:

```bash
mvn -Dtest=ClassNameTest test
mvn clean verify
npm run lint
```

Documentation-only changes should be validated by re-reading the touched docs and checking that links, names, and runtime baselines remain consistent.

## Pull request checklist

- Ticket scope still matches the code change.
- The narrowest relevant validation passed.
- [docs/IMPLEMENTATION_STATUS.md](./docs/IMPLEMENTATION_STATUS.md) reflects the new state.
- Any governance or API document touched by the change is updated in the same branch.