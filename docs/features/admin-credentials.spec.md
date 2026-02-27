# Feature Specification: Admin Credentials Security Hardening

## 1. Summary

Remove hardcoded admin credentials from source-controlled files and from `@Value`
default expressions. In the `prod` Spring profile, credentials must be supplied via
environment variables; startup must fail fast if they are absent or if the known weak
defaults are still in use.

## 2. Problem Statement

Three locations currently expose admin credentials in source control:

| Location | Exposure |
|---|---|
| `DataInitializer.java` `@Value("${…:ChangeMe123!}")` | Password embedded in compiled bytecode and source history |
| `DataInitializer.java` `@Value("${…:admin@wordai.local}")` | Email embedded in source history |
| `application.properties` `wordai.admin.password=ChangeMe123!` | Password in plaintext property file |

If a developer clones the repo and deploys with `--spring.profiles.active=prod` without
setting environment variables, the application starts silently with the well-known weak
credentials. This is a critical security risk.

## 3. Desired Behaviour

### Dev profile (default)
- Credentials fall back to safe dev-only defaults via `application.properties`.
- No change to the local development workflow.

### Prod profile
- `application-prod.properties` resolves credentials from environment variables
  (`WORDAI_ADMIN_EMAIL`, `WORDAI_ADMIN_PASSWORD`, `WORDAI_ADMIN_USERNAME`,
  `WORDAI_ADMIN_FULLNAME`) with **no fallback**.
- If an environment variable is not set, Spring's placeholder resolution throws
  `IllegalArgumentException` before the application context finishes loading.
- `AdminCredentialsValidator` performs an additional defence-in-depth check: if the
  resolved password equals the known default (`ChangeMe123!`) or is blank, it throws
  `IllegalStateException` and aborts startup, even if an env var was set to the same
  value.

## 4. Lock Design

| Component | Change |
|---|---|
| `DataInitializer.java` | Remove `:defaultValue` fallbacks from `@Value` annotations |
| `application.properties` | Change plain values to `${WORDAI_ADMIN_*:dev-default}` — keeps dev workflow intact |
| `application-prod.properties` | Add `wordai.admin.*=${WORDAI_ADMIN_*}` with **no** fallback |
| `AdminCredentialsValidator` (**new**) | `@Component` that runs `@PostConstruct`; checks profile and rejects weak/blank passwords in prod |

## 5. Acceptance Tests (TDD)

| # | Scenario | Assertion |
|---|---|---|
| T1 | Prod profile, password = `ChangeMe123!` (known default) | Validator throws `IllegalStateException` |
| T2 | Prod profile, email blank | Validator throws `IllegalStateException` |
| T3 | Prod profile, strong credentials | Validator passes without exception |
| T4 | Dev profile, credentials = known defaults | Validator passes (no exception) |

## 6. Files Changed

| File | Change |
|------|--------|
| `DataInitializer.java` | Remove hardcoded `:default` values from four `@Value` annotations |
| `application.properties` | Replace plain credentials with env-var expressions + dev fallback |
| `application-prod.properties` | Add four `wordai.admin.*=${WORDAI_ADMIN_*}` entries (no fallback) |
| `AdminCredentialsValidator.java` | **New** — prod-profile fail-fast credential check |
| `AdminCredentialsValidatorTest.java` | **New** — TDD tests for the validator |
| `docs/IMPLEMENTATION_STATUS.md` | Record completion |
