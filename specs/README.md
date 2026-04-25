# Architecture Ticket Backlog

This folder tracks the architecture improvement backlog as one markdown file per ticket.

Use [TEMPLATE.md](./TEMPLATE.md) for every new backlog ticket so status, scope, validation, and acceptance criteria stay consistent.

## Ticket Order

1. [ARCH-01 Move Shared Dictionary Config Types](ARCH-01-move-shared-dictionary-config-types.md) `completed`
2. [ARCH-02 Move Analysis Result Models](ARCH-02-move-analysis-result-models.md) `completed`
3. [ARCH-03 Break Core Bot Package Cycle](ARCH-03-break-core-bot-package-cycle.md) `completed`
4. [ARCH-12 Tighten Architecture Fitness Coverage](ARCH-12-tighten-architecture-fitness-coverage.md) `completed`
5. [ARCH-04 Remove Legacy Runtime Ownership](ARCH-04-remove-legacy-runtime-ownership.md) `completed`
6. [ARCH-05 Make DictionaryService Single Boundary](ARCH-05-make-dictionaryservice-single-boundary.md) `completed`
7. [ARCH-06 Stop Controller Reach-Through](ARCH-06-stop-controller-reach-through.md) `completed`
8. [ARCH-07 Split Resource Controllers](ARCH-07-split-resource-controllers.md) `completed`
9. [ARCH-08 Remove Placeholder Stats Endpoints](ARCH-08-remove-placeholder-stats-endpoints.md) `completed`
10. [ARCH-09 Remove Placeholder Premium Analytics Endpoints](ARCH-09-remove-placeholder-premium-analytics-endpoints.md) `completed`
11. [ARCH-10 Unify Algorithm Metadata](ARCH-10-unify-algorithm-metadata.md) `completed`
12. [ARCH-11 Align Feature Toggles With Registry](ARCH-11-align-feature-toggles-with-registry.md) `completed`
13. [ARCH-13 Refresh Architecture Documentation](ARCH-13-refresh-architecture-documentation.md) `completed`
14. [ARCH-14 Establish Governance Source of Truth](ARCH-14-establish-governance-source-of-truth.md) `completed`
15. [ARCH-15 Enforce Spec-Driven Workflow](ARCH-15-enforce-spec-driven-workflow.md) `completed`
16. [ARCH-16 Harden Build Validation Gates](ARCH-16-harden-build-validation-gates.md) `completed`
17. [ARCH-17 Fix API Boundary Validation](ARCH-17-fix-api-boundary-validation.md) `completed`
18. [ARCH-18 Standardize Error and Failure Handling](ARCH-18-standardize-error-and-failure-handling.md) `completed`
19. [ARCH-19 Raise High-Risk Server Coverage](ARCH-19-raise-high-risk-server-coverage.md) `completed`
20. [ARCH-20 Remove WordEntropy Legacy Paths](ARCH-20-remove-wordentropy-legacy-paths.md) `completed`
21. [ARCH-21 Decompose GameSession Responsibilities](ARCH-21-decompose-gamesession-responsibilities.md) `completed`
22. [ARCH-22 Add Entropy Memoization](ARCH-22-add-entropy-memoization.md) `completed`
23. [ARCH-23 Move Feature Toggles to Runtime Policy](ARCH-23-move-feature-toggles-to-runtime-policy.md) `completed`
24. [ARCH-24 Introduce API Versioning](ARCH-24-introduce-api-versioning.md) `completed`
25. [ARCH-25 Complete Public Interface Documentation](ARCH-25-complete-public-interface-documentation.md) `completed`
26. [ARCH-26 Decompose Remaining Server Coordination Classes](ARCH-26-decompose-remaining-server-coordination-classes.md) `completed`
27. [ARCH-27 Normalize Test Conventions](ARCH-27-normalize-test-conventions.md) `completed`
28. [ARCH-28 Tighten EntropyKey Encapsulation](ARCH-28-tighten-entropykey-encapsulation.md) `completed`

## First Sprint Candidate

- ARCH-01 `completed`
- ARCH-02 `completed`
- ARCH-03 `completed`
- ARCH-12 `completed`

## Next Remediation Wave

- ARCH-14 `completed`
- ARCH-15 `completed`
- ARCH-16 `completed`
- ARCH-17 `completed`
- ARCH-18 `completed`
- ARCH-19 `completed`
- ARCH-20 `completed`
- ARCH-21 `completed`
- ARCH-22 `completed`
- ARCH-23 `completed`
- ARCH-24 `completed`

## Working Rules

- Preserve existing public API routes unless a ticket explicitly says otherwise.
- Prefer test-first refactors that tighten architecture enforcement as code moves.
- Treat the Spring runtime path as the only supported production path.
- Remove or hide unfinished placeholder endpoints instead of keeping fake responses in production.
- Prefer small, acceptance-tested remediation tickets over large mixed cleanup branches.
- Create new tickets from [TEMPLATE.md](./TEMPLATE.md) and keep `Status`, `Validation`, and `Acceptance Criteria` sections intact.
