# Development Guide

Welcome to the WordAI development documentation. This section covers architecture, setup, and development practices.

## Architecture Overview

WordAI is built on modern Java and Spring Boot technologies:

```
┌─────────────────────────────────────────────────┐
│           Web Browser (User Interface)           │
└──────────────────┬──────────────────────────────┘
                   │ HTTPS
                   ▼
┌─────────────────────────────────────────────────┐
│      Spring Boot REST API (Port 8080)            │
├─────────────────────────────────────────────────┤
│  Controllers                                     │
│  ├─ WordGameController (Game lifecycle)          │
│  ├─ DictionaryController (Dictionary catalog)    │
│  ├─ AnalysisController (Analysis runs)           │
│  ├─ AlgorithmController (Algorithm catalog)      │
│  └─ HistoryController (User history)             │
├─────────────────────────────────────────────────┤
│  Services                                        │
│  ├─ WordGameService (Session orchestration)      │
│  ├─ DictionaryService (Loaded dictionaries)      │
│  ├─ AlgorithmFeatureService (API exposure policy)│
│  └─ GameHistoryService (Persisted history)       │
├─────────────────────────────────────────────────┤
│  Core Components                                 │
│  ├─ WordGame (Game engine primitives)            │
│  ├─ Dictionary (Word management)                 │
│  ├─ WordEntropy / ResponseMatrix (shared caches) │
│  ├─ WordGamePlayer (Bot implementations)         │
│  └─ SelectionAlgo (Guessing strategies)          │
├─────────────────────────────────────────────────┤
│  Data Persistence                                │
│  └─ H2 Database + in-memory session cache        │
└─────────────────────────────────────────────────┘
```

## Key Technologies

- **Java 25** - local development and CI baseline
- **Java 17** - cloud Maven profile for deployment compatibility
- **Temurin 21 container image** - current Docker baseline until container alignment work lands
- **Spring Boot 3.5.13** - REST API framework
- **Maven 3.8.9+** - Build automation
- **H2 Database** - Embedded SQL database
- **JUnit 5** - Unit testing

## Development Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/jchanning/wordai.git
   cd wordai
   ```

2. **Build the project**
   ```bash
   mvn clean compile
   ```

3. **Run tests**
   ```bash
   mvn clean verify
   ```

4. **Lint frontend static assets**
   ```bash
   npm run lint
   ```

5. **Start development server**
   ```bash
   mvn spring-boot:run
   ```

## Project Structure

```
wordai/
├── src/main/java/com/fistraltech/
│   ├── core/              # Game primitives
│   ├── bot/               # Bot strategies
│   ├── analysis/          # Analytics
│   └── server/            # Spring Boot API
├── src/main/resources/
│   ├── static/            # Web UI files
│   └── dictionaries/      # Word lists
├── src/test/java/         # Unit tests
├── docs/                  # Complete documentation
└── deployment/            # Deployment scripts
```

## Related Documentation

- [Architecture](../ARCHITECTURE.md)
- [Execution Playbook](../EXECUTION_PLAYBOOK.md)
- [Coding Standards](../coding-standards.md)
- [Contribution Guide](../../CONTRIBUTING.md)
- [Java 25 Upgrade Notes](./java-upgrade-notes.md)
- [Performance Optimization](./performance-optimization.md)
- [Deployment Guide](../deployment/deployment-guide.md)

## Contributing

Use [../../CONTRIBUTING.md](../../CONTRIBUTING.md) for the minimum ticket, test, and status workflow.
Use [../EXECUTION_PLAYBOOK.md](../EXECUTION_PLAYBOOK.md) when the change affects architecture, API shape, validation policy, or repository governance.
Current CI also enforces frontend lint plus a staged 60% JaCoCo line-coverage floor during `mvn clean verify`.
