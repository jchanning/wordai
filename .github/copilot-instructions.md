# WordAI Copilot Instructions

WordAI is a Wordle-like word guessing game AI system with sophisticated bot strategies and analytics. This system simulates and analyzes different word-guessing algorithms.

## Architecture Overview

### Core Game Engine (`com.fistraltech.core`)
- **WordGame**: Main game engine handling guesses and responses
- **Dictionary**: Word management with filtering capabilities and character indexing
- **Response/ResponseEntry**: Encodes guess feedback using 'G'(Green), 'A'(Amber), 'R'(Red) status codes
- **WordSource**: Loads words from dictionary files

### Bot System (`com.fistraltech.bot`)
- **Player interface**: Defines bot behavior with `playGame()` method
- **WordGamePlayer**: Main bot implementation using pluggable selection algorithms
- **SelectionAlgo**: Abstract base for word selection strategies (SelectRandom, SelectMostCommonLetters, SelectFixedFirstWord)
- **Filter**: Core filtering logic that eliminates impossible words based on game responses
- **FilterCharacters**: Manages valid characters per position

### Analytics & Analysis (`com.fistraltech.analysis`)
- **GameAnalytics**: Captures game metrics and exports to CSV files
- **DictionaryAnalyser**: Analyzes word patterns and letter frequencies
- **ComplexityAnalyser**: Measures algorithm performance

## Key Patterns

### Response Status Codes
```java
// Game feedback encoding
'G' = Green (correct letter, correct position)
'A' = Amber (correct letter, wrong position) 
'R' = Red (letter not in word)
```



### Filter Update Pattern
When processing game responses, the Filter class updates position-based character constraints:
- Green letters: Remove all other letters from that position
- Amber letters: Remove letter from guessed position, add to mustContain
- Red letters: Remove letter from all positions

### Selection Algorithm Pattern
All bots extend `SelectionAlgo` and implement:
```java
abstract String selectWord(Response lastResponse, Dictionary dictionary);
```

## Build & Development

### Maven Configuration
- **Source**: `src/main/java` (Maven standard)
- **Tests**: `src/test/java` with JUnit 5
- **Build**: `mvn clean compile` or VS Code Java extensions
- **Run**: Execute `Main.playMultipleGames()` for batch simulation

### Configuration System
- **Main config**: `src/main/resources/application.properties`
- **User config**: `wordai.properties` (optional override)
- **Fallback dictionary**: `src/main/resources/dictionaries/5_letter_words.txt`
- **ConfigManager**: Handles path resolution and validation

### Configuration Properties
```properties
# Dictionary configuration
dictionary.base.path=${user.home}/OneDrive/Projects/Wordlex
dictionary.file.name=5_Letter_Words_Official.txt
dictionary.fallback.paths=dictionaries/5_letter_words.txt

# Game settings
game.word.length=5
game.max.attempts=6
game.simulation.iterations=1
```

### Common Issues
- **Configuration validation**: Check `ConfigManager.validateConfiguration()` for setup issues
- **Missing dictionary**: System falls back to `src/main/resources/dictionaries/5_letter_words.txt`
- **ArrayIndexOutOfBounds**: Usually in word length validation or filter array access
- **Missing classes**: Ensure all core classes exist before running simulations

## Testing & Analytics

### Simulation Output
Games generate timestamped CSV files:
- `summary-{timestamp}.csv`: Game outcomes and attempt counts
- `details-{timestamp}.csv`: Detailed move-by-move analysis
- `columns-{timestamp}.csv`: Dictionary size reduction tracking

### Performance Metrics
Key analytics focus on:
- Average attempts per word
- Dictionary reduction efficiency
- Algorithm comparison across word sets

## VS Code Development

### Build Commands
- `Ctrl+Shift+P` → "Java: Rebuild Projects"
- Terminal: `mvn clean compile`
- Create `.vscode/tasks.json` for custom build tasks

### Common Debugging
Add debugging to `Main.playMultipleGames()` to trace ArrayIndexOutOfBounds:
```java
System.out.println("Processing word: " + targetWord + " (length: " + targetWord.length() + ")");
```

## Project Conventions

- **Package structure**: Strict separation of core game logic, bot implementations, and analysis tools
- **Error handling**: InvalidWordException for game rule violations
- **Configuration**: Config class for game parameters (max attempts, etc.)
- **Data persistence**: Optional SQL Server DAO classes for result storage
