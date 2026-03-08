# Architecture Ticket Backlog

This folder tracks the architecture improvement backlog as one markdown file per ticket.

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

## First Sprint Candidate

- ARCH-01 `completed`
- ARCH-02 `completed`
- ARCH-03 `completed`
- ARCH-12 `completed`

## Working Rules

- Preserve existing public API routes unless a ticket explicitly says otherwise.
- Prefer test-first refactors that tighten architecture enforcement as code moves.
- Treat the Spring runtime path as the only supported production path.
- Remove or hide unfinished placeholder endpoints instead of keeping fake responses in production.