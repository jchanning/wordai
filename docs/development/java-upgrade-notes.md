# Java Upgrade Notes

## Current Baseline

As of April 2026, WordAI uses a mixed runtime baseline:

- Java 25 for local development and CI
- Java 17 for the `cloud` Maven profile
- Temurin 21 in the current Dockerfile
- Spring Boot 3.5.13 across the Maven build

## Upgrade Summary

| Component | Previous baseline | Current baseline |
|---|---|---|
| JDK / bytecode target | Java 21 | Java 25 |
| Spring Boot | 3.4.0 | 3.5.13 |
| Spring Security | 6.4.x | 6.5.9 |
| JaCoCo | 0.8.12 | 0.8.13 |
| ArchUnit | 1.3.0 | 1.4.1 |
| springdoc-openapi | 2.6.0 | 2.8.16 |

## Notes

- Spring Boot 3.5.13 is required for Java 25 bytecode support.
- The cloud profile remains on Java 17 for deployment compatibility.
- The Dockerfile still uses Temurin 21, so container work should treat that as a separate baseline until the image is aligned with the Maven default profile.
- `pom.xml` is the authoritative source for current compiler and dependency versions.

## Validation Commands

```bash
mvn clean verify
mvn verify -P security -DskipTests
```

## Historical Note

Older documentation that referenced Java 21 and Spring Boot 3.4 represented the pre-April-2026 baseline and should now be treated as superseded.