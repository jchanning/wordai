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

- **Java 21** (or 17 for production)
- **Spring Boot 3.4.0** - REST API framework
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
   mvn clean test
   ```

4. **Start development server**
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

- [Java 21 Upgrade Notes](./java-upgrade-notes.md)
- [Performance Optimization](./performance-optimization.md)
- [Deployment Guide](../deployment/deployment-guide.md)

## Contributing

See [docs/README.md](../README.md) for contribution guidelines.
