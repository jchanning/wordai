# Feature Specification: Flyway Schema Migration (`ddl-auto=update` removal)

## 1. Summary

Replace `spring.jpa.hibernate.ddl-auto=update` in the production configuration with
`validate` and manage all schema changes with versioned **Flyway** migration scripts.

## 2. Problem Statement

`ddl-auto=update` is a well-known production antipattern:

- Hibernate's DDL diffing silently ignores some changes (column type changes, dropped
  columns, index changes) and can cause data loss on others.
- It provides no audit trail — there is no record of *when* or *what* changed.
- It makes rollback impossible: there is no down-migration capability.
- It is implicitly trusted over a controlled, reviewed migration — a significant
  operational risk.

The production database at `jdbc:h2:file:/home/opc/wordai-data/wordai` already has data
and tables created by `ddl-auto=update`. Those tables must not be re-created; only future
incremental changes should be applied.

## 3. Fix Design

| Component | Change |
|---|---|
| `pom.xml` | Add `flyway-core:9.22.3` |
| `src/main/resources/db/migration/V1__baseline.sql` | **New** — full DDL for the current schema |
| `application.properties` (dev) | Add `spring.flyway.enabled=false` — dev still uses `ddl-auto=update` for convenience |
| `application-prod.properties` | Change `ddl-auto=update` → `ddl-auto=validate`; add `spring.flyway.enabled=true` and `spring.flyway.baseline-on-migrate=true` |

### Why `baseline-on-migrate=true`?

The production database already has tables with no `flyway_schema_history` table.
`baseline-on-migrate=true` tells Flyway:

> "If the schema history table does not exist, create it and mark V1 as already applied."

This means the V1 script is **not executed** on the existing production database (the tables
already exist); it is only applied to fresh databases (e.g., new environments, CI runs, tests).

### Why keep `ddl-auto=update` in dev?

The dev H2 file-backed database (`jdbc:h2:file:./data/wordai`) is a throwaway environment.
Keeping `ddl-auto=update` there preserves fast iteration without requiring a migration script
for every field added during development. Migration scripts only need to be created when a
change is ready to ship to production.

## 4. Schema — V1 Baseline

Two entities, three tables:

| Table | JPA Entity |
|---|---|
| `users` | `User` |
| `user_roles` | `User.roles` (`@ElementCollection`) |
| `player_games` | `PersistedGame` |

Full DDL in `src/main/resources/db/migration/V1__baseline.sql`.

## 5. Acceptance Tests (TDD)

| # | Scenario | Assertion |
|---|---|---|
| T1 | Spring context loads with Flyway enabled + `ddl-auto=validate` (fresh in-memory H2) | Context loads without exception |
| T2 | After V1 migration, `users` table exists with expected columns | JDBC metadata confirms columns |
| T3 | After V1 migration, `user_roles` table exists | JDBC metadata confirms table |
| T4 | After V1 migration, `player_games` table exists | JDBC metadata confirms table |

## 6. Future Migrations

All future schema changes must be delivered as new numbered migration scripts:

```
src/main/resources/db/migration/
  V1__baseline.sql          ← current schema
  V2__add_foo_column.sql    ← example future migration
```

Never modify a migration script that has already been applied to any environment.
Use `V<n>__description.sql` naming, incrementing `n` monotonically.

## 7. Files Changed

| File | Change |
|------|--------|
| `pom.xml` | Add `flyway-core:9.22.3` |
| `src/main/resources/db/migration/V1__baseline.sql` | **New** — baseline DDL |
| `application.properties` | Add `spring.flyway.enabled=false` |
| `application-prod.properties` | `ddl-auto=validate`, `spring.flyway.enabled=true`, `spring.flyway.baseline-on-migrate=true` |
| `src/test/java/com/fistraltech/FlywayMigrationTest.java` | **New** — TDD tests |
| `docs/IMPLEMENTATION_STATUS.md` | Record completion |
