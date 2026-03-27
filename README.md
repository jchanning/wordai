# WordAI - Intelligent Word Guessing Game with Advanced Bot Strategies

[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**WordAI** is a sophisticated word-guessing game (similar to Wordle) with an intelligent bot system that implements and compares multiple word selection algorithms. The project includes a Spring Boot REST API, interactive web interface, and comprehensive analytics for algorithm performance evaluation.

## 🎯 Features

### Interactive Web Game
- Clean, modern UI with real-time feedback
- Multiple dictionary options (4, 5, and 6-letter words)
- Session statistics and game history tracking
- AI-powered word suggestions with multiple strategies

### Intelligent Bot System
Multiple bot algorithms for automated gameplay:
- **Random Selection** - Baseline strategy
- **Maximum Entropy** - Information theory-based optimization
- **Bellman Full Dictionary** - Bellman-optimal across complete dictionary

### Comprehensive Analytics
- CSV export of game metrics
- Dictionary reduction tracking
- Algorithm performance comparison
- Letter frequency analysis

### REST API
- Full-featured Spring Boot REST API
- Session management
- Multiple selection algorithm support
- Real-time game state updates

## 🚀 Quick Start

### Prerequisites
- Java 21 or higher
- Maven 3.8.9+ (or use included Maven wrapper)

### Running the Application

```bash
# Clone the repository
git clone https://github.com/jchanning/wordai.git
cd wordai

# Run with Maven
mvn spring-boot:run

# Or use Maven Wrapper (Windows)
mvnw.cmd spring-boot:run
```

The application will start on `http://localhost:8080`

### Running with Docker

```bash
# Build and run with Docker Compose
cp .env.example .env
docker compose up --build
```

The application will be available at `http://localhost:8080`.

Containerization files:
- `Dockerfile` - multi-stage build using Java 21
- `docker-compose.yml` - local runtime with persistent named volumes
- `docker/wordai.properties` - container-specific configuration for `ConfigManager`
- `.dockerignore` - optimized build context

Persistent volumes created by Compose:
- `wordai-data` mounted at `/app/data` (H2 database and generated output)
- `wordai-logs` mounted at `/app/logs`

### Access from Other Devices

The server is configured to accept connections from your local network:
```
http://<YOUR-IP-ADDRESS>:8080
```

Use `ipconfig` (Windows) or `ifconfig` (macOS/Linux) to find your IP address.

## 📊 Project Structure

```
WordAI/
├── src/main/java/com/fistraltech/
│   ├── core/              # Core game engine
│   │   ├── WordGame.java  # Main game logic
│   │   ├── Dictionary.java # Word management
│   │   ├── Response.java  # Game feedback
│   │   ├── Filter.java    # Word filtering logic
│   │   ├── WordEntropy.java # Entropy computation
│   │   ├── ResponseMatrix.java # Pre-computed response matrix
│   │   └── Column.java    # Letter position analysis
│   ├── bot/               # Bot implementations
│   │   ├── WordGamePlayer.java  # Main bot player
│   │   └── selection/     # Selection algorithms
│   ├── server/            # Spring Boot REST API
│   │   ├── controller/    # REST endpoints
│   │   ├── dto/           # Data transfer objects
│   │   ├── model/         # Domain models
│   │   ├── algo/          # Algorithm registry & descriptors
│   │   └── repository/    # JPA repositories
│   ├── security/          # Spring Security & OAuth2
│   ├── analysis/          # Analytics tools
│   └── util/              # Configuration utilities
├── src/main/resources/
│   ├── application.properties  # Configuration
│   ├── dictionaries/      # Word dictionaries
│   └── static/            # Web UI (HTML/CSS/JS)
└── src/test/java/         # Unit tests
```

## 🎮 How to Play

1. **Start a New Game** - Click "New Game" and select word length (4-6 letters)
2. **Make Guesses** - Type your guess and click "Make Guess"
3. **Interpret Feedback**:
   - 🟢 **Green**: Correct letter in correct position
   - 🟡 **Amber**: Correct letter, wrong position
   - 🔴 **Red**: Letter not in the word
4. **Use AI Assistance** - Select a strategy and click "Get Suggestion"
5. **Track Progress** - View session stats and game history in the left panel

## 🤖 Bot Algorithms

### SelectRandom
Simple baseline that randomly chooses from valid words.
- **Average**: ~5-6 attempts
- **Use case**: Baseline for comparison

### SelectMaximumEntropy
Uses information theory to maximize information gain per guess.
- **Average**: ~3-4 attempts
- **Use case**: Optimal strategy (computationally intensive)

### SelectBellmanFullDictionary
Bellman-optimal strategy that considers the full dictionary for each decision.
- **Average**: ~3-4 attempts
- **Use case**: Highest-quality play across all game states

## 🔧 Configuration

Create a `wordai.properties` file in the project root (see `wordai.properties.example`):

```properties
# Dictionary configuration
dictionary.base.path=${user.home}/OneDrive/Projects/Wordlex
dictionary.file.name=5_Letter_Words_Official.txt

# Game settings
game.word.length=5
game.max.attempts=6

# Server configuration
server.port=8080
server.address=0.0.0.0  # Allow network access
```

## 📈 Analytics & Research

The project generates detailed CSV analytics:

### Summary CSV
```csv
Iteration,Algorithm,Target,Attempts,Status
1,MaximumEntropy,AROSE,3,WON
```

### Details CSV
```csv
Iteration,Attempt,Guess,Response,RemainingWords
1,1,CRANE,RAARG,128
1,2,STALE,AAGGG,5
```

### Column Analysis
Tracks letter frequency distributions per position for statistical analysis.

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=WordGameServiceTest

# Generate test coverage report
mvn clean test jacoco:report
```

## 📡 API Documentation

### Create Game
```http
POST /api/wordai/games
Content-Type: application/json

{
  "wordLength": 5,
  "dictionaryOption": "default"
}
```

### Make Guess
```http
POST /api/wordai/games/{gameId}/guess
Content-Type: application/json

{
  "word": "CRANE"
}
```

### Get Suggestion
```http
GET /api/wordai/games/{gameId}/suggestion?strategy=ENTROPY
```

See API documentation at `http://localhost:8080/swagger-ui.html` when running.

## 🏗️ Architecture

### Core Game Engine
The `com.fistraltech.core` package contains the fundamental game logic:
- Wordle-like feedback system (Green/Amber/Red status codes)
- Efficient dictionary management with column-based indexing
- Position-based character filtering

### Bot System
The `com.fistraltech.bot` package implements the Strategy pattern:
- Pluggable selection algorithms
- Filter-based word elimination
- Performance tracking and analytics

### Web Layer
Spring Boot REST API with:
- Stateful session management
- CORS support for web clients
- Real-time game state updates

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👤 Author

**Fistral Technologies**

## 🙏 Acknowledgments

- Inspired by Wordle by Josh Wardle
- Built with Spring Boot and modern web technologies
- Information theory concepts for entropy-based algorithms

## 📚 Documentation

For detailed class-level documentation, see:
- [Documentation Status](DOCUMENTATION_STATUS.md) - Overview of documentation coverage
- JavaDoc - Generate with `mvn javadoc:javadoc`

## 🐛 Known Issues

See the [Issues](https://github.com/jchanning/wordai/issues) page for current bugs and feature requests.

## � Documentation

**Complete documentation is available in the [`docs/`](./docs/) directory:**

- 🚀 **[Getting Started](./docs/getting-started/)** - Quick start and setup
- 👤 **[User Guide](./docs/user-guides/)** - How to play and use the application
- 🛠️ **[Deployment](./docs/deployment/)** - Oracle Cloud, HTTPS, DNS, and other deployment guides
- 💻 **[Development](./docs/development/)** - Architecture, Java 21 upgrade, performance optimization
- ✨ **[Features](./docs/features/)** - Game history, UI improvements, and planned features
- 📦 **[Releases](./docs/releases/)** - Release notes and deployment reports
- ❓ **[Troubleshooting](./docs/troubleshooting/)** - Common issues and solutions

**For a complete index of all documentation**, see [docs/README.md](./docs/README.md).

## �📊 Stats

![Code Size](https://img.shields.io/github/languages/code-size/jchanning/wordai)
![Repo Size](https://img.shields.io/github/repo-size/jchanning/wordai)

---

**Happy Guessing! 🎯**
