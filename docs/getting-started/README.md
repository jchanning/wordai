# Getting Started with WordAI

Welcome to WordAI! This guide will help you get up and running quickly.

## What is WordAI?

WordAI is an intelligent word-guessing game (similar to Wordle) with:
- **Interactive Web UI** - Play against AI or in single-player mode
- **Intelligent Bot System** - Multiple AI algorithms compete
- **Advanced Analytics** - Track algorithm performance
- **REST API** - Full programmatic access

## Quick Start

### Prerequisites

- **Java 21** or higher (for development)
- **Java 17** (for Oracle Cloud deployment)
- **Maven 3.8.9+** (or use Maven wrapper)

### Running Locally

```bash
# Clone the repository
git clone https://github.com/jchanning/wordai.git
cd wordai

# Run the application
mvn spring-boot:run

# Access at: http://localhost:8080
```

### Running with Production Profile

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=prod"
```

---

## System Requirements

| Component | Requirement |
|-----------|-------------|
| **Java** | 21 (development) or 17 (production) |
| **Maven** | 3.8.9+ |
| **Database** | H2 (embedded, no setup needed) |
| **Memory** | Minimum 512MB, Recommended 2GB+ |
| **Disk Space** | ~200MB for build artifacts |

---

## Next Steps

- **Want to play?** → See [User Guide](../user-guides/USER_GUIDE.md)
- **Want to deploy?** → See [Deployment Guide](../deployment/deployment-guide.md)
- **Want to develop?** → See [Development Guide](../development/)
- **Need help?** → See [Troubleshooting](../troubleshooting/)

For more details, see the complete [README](./overview.md).
