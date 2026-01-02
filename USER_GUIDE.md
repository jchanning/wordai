# WordAI User Guide

Welcome to **WordAI** - A sophisticated Wordle-like game with advanced bot analysis and dictionary exploration tools.

## Table of Contents
- [Quick Start](#quick-start)
- [Game Modes](#game-modes)
- [Authentication](#authentication)
- [How to Play](#how-to-play)
- [Bot Analysis](#bot-analysis)
- [Dictionary Explorer](#dictionary-explorer)
- [Understanding Game Feedback](#understanding-game-feedback)
- [Tips & Strategies](#tips--strategies)

---

## Quick Start

1. **Access the Application**: Navigate to `http://localhost:8080` (or your deployed URL)
2. **Start Playing**: The app opens in Play mode - start guessing immediately!
3. **Optional Login**: Click the green "Sign In" link in the menu to create an account and track your games

---

## Game Modes

WordAI offers five distinct modes accessible from the navigation menu:

### Play Mode
Interactive word game where you have to guess the hidden word.
- Select the word length: 4, 5, 6 or 7 letters.
- 6 attempts to guess the word
- Color-coded feedback after each guess
- Real-time dictionary validation
- Get assistance by getting a suggestion based on one of the algorithmic strategies (Random, Letter Frequency, etc.)

### Auto Mode (Bot Demo)
Watch an AI bot play the game automatically.
- Select a bot strategy from the dropdown
- Choose number of games to simulate
- Watch step-by-step gameplay
- View success rate and statistics

### Analyse Mode (Bot Performance)
Comprehensive analysis of bot performance across the entire dictionary of letters.
- Run simulated games covering the full dicitonary
- Compare the performance of different strategies with dictionaries of 4, 5, 6 or 7 letter words
- View win rates, average guesses, and failure rates
- Analyze performance patterns
- Export results to CSV

### Dictionary Mode
Explore and analyze word dictionaries.
- View all valid words for different lengths (4-7 letters)
- Sort by word, frequency, or column statistics
- Search and filter words
- Export dictionary data to CSV
- View letter position statistics with interactive bar charts to assist with your guessing strategy

### Admin Mode
Administrative functions (placeholder for future features).

---

## Authentication

**Login is optional!** You can play as a guest or create an account to track your games.

### Playing as a Guest
- Full access to all game modes
- Games are not saved to your profile
- No registration required

### Creating an Account
1. Click the green **"Sign In"** link in the menu
2. Click **"Sign up"** on the login page
3. Fill in your details:
   - Email address
   - Username
   - Full name
   - Password (minimum 6 characters)
4. Click **"Create Account"**

### Signing In
1. Click **"Sign In"** in the menu
2. Enter your username/email and password
3. Click **"Sign In"**

Once logged in:
- Your name appears in the top right corner
- Games can be tracked to your account (future feature)
- Click **"Logout"** to sign out

### OAuth Options
Google and Apple Sign-In are available but require configuration (see [AUTHENTICATION_SETUP.md](AUTHENTICATION_SETUP.md)).

---

## How to Play

### Basic Gameplay

1. **Start a Game**
   - Navigate to Play mode
   - A random target word is selected
   - You have 6 attempts to guess it

2. **Make a Guess**
   - Type a word in the input field
   - Click **"Submit Guess"** or press Enter
   - Only valid dictionary words are accepted

3. **Read the Feedback**
   - **Green**: Letter is correct and in the right position
   - **Yellow**: Letter is in the word but wrong position
   - **Gray**: Letter is not in the word or the letter appears more times than in the target

4. **Win or Lose**
   - Win: Guess the word within 6 attempts
   - Lose: Use all 6 attempts without finding the word
   - The target word is revealed after the game ends

### Strategy Selection

Choose how the bot (or your hints) should suggest words:
- **Random**: Randomly select from valid words
- **Maximize Entropy**: Choose words that maximize information gain
- **Most Common Letters**: Prioritizes choosing words randomly from a subset of words with the most common letters
- **Minimize Column Length**: Strategy that aims to minimise the number of remaining choices for each column after the guess
- **Dictionary Reduction**: Works in a similar way to Maximise Entropy, the objective function identifies the word that would filter the dictionary to the smallest size.

### Game Controls

- **New Game**: Start a fresh game with a new target word
- **Give Up**: Reveal the target word and end the current game
- **Change Strategy**: Switch bot strategy mid-game (Play mode)

---

## Bot Analysis

### Running Auto Mode (Bot Demo)

1. Navigate to **Auto** mode
2. Select a **Strategy** from the dropdown
3. Choose **Number of Games** (1, 10, or 100)
4. Click **"Start Auto-Play"**
5. Watch the bot play in real-time

**What You'll See:**
- Each guess the bot makes
- Color-coded feedback for each guess
- Win/loss outcome
- Statistics: Games played, wins, losses, success rate
- Full game history with all guesses

### Running Performance Analysis

1. Navigate to **Analyse** mode
2. Select a **Strategy** to test
3. Choose **Number of Games** (100, 500, 1000, 5000)
4. Click **"Run Analysis"**

**Analysis Results Include:**
- Total games simulated
- Win rate (%)
- Average guesses per successful game
- Failure rate (games requiring >6 guesses)
- Detailed statistics table
- Option to export results to CSV

### Comparing Strategies

Run analysis for multiple strategies to compare:
- Which strategy has the highest win rate?
- Which finds the answer fastest on average?
- Which has the lowest failure rate?

---

## Dictionary Explorer

### Viewing Words

1. Navigate to **Dictionary** mode
2. Select word length (4, 5, 6, or 7 letters) from dropdown
3. Browse the complete list of valid words

### Sorting & Filtering

**Sort Options:**
- Click column headers to sort
- Sort by: Word, Position 1-7 letters, Frequency
- Click again to reverse sort order

**Search:**
- Use the search box to filter words
- Searches across all columns
- Real-time filtering as you type

**Clear:**
- Click **"Clear"** to reset search and view all words

### Understanding Statistics

**Frequency Column:**
- Shows how often letters appear in that position
- Higher numbers = more common letter patterns

**Position Columns:**
- Each column shows which letters appear in that position
- Helps identify common starting letters, endings, etc.

### Exporting Data

- Click **"Export to CSV"** to download the current dictionary
- Includes all words and statistics
- Opens in Excel, Google Sheets, or any CSV viewer

### Viewing Visualizations

- Click **"Show Charts"** to see letter frequency bar charts
- Visual representation of letter distribution by position
- Helps identify optimal starting words
- Click **"Hide Charts"** to collapse

---

## Understanding Game Feedback

### Response Codes

Each letter in your guess receives a code:

| Code | Color | Meaning | Example |
|------|-------|---------|---------|
| **G** | Green | Correct letter, correct position | Target: SLATE, Guess: **S**TONE → S=**G** |
| **A** | Yellow/Amber | Correct letter, wrong position | Target: SLATE, Guess: P**A**RTS → A=**A** |
| **R** | Gray/Red | Letter not in target word | Target: SLATE, Guess: PROU**D** → D=**R** |
| **X** | Depends | Letter exists but used too many times | Target: SLATE, Guess: A**LL**OW → Second L=**X** |

### Complex Scenarios

**Duplicate Letters:**
- If target has 1 'E' and you guess 2 'E's:
  - First 'E' gets G (if correct position) or A (if wrong position)
  - Second 'E' gets X (excess)

**Example:**
- Target: **STALE** (one E)
- Guess: **STEEL** (two E's)
- Result: S=G, T=G, first E=A, second E=X, L=A

---

## Tips & Strategies

### For Players

1. **Start with Common Letters**
   - Use words with E, A, R, O, T, I
   - Example starting words: STARE, AROSE, LATER, IRATE

2. **Maximize Information**
   - Use all unique letters in early guesses
   - Avoid repeating letters until you must

3. **Use Position Information**
   - Pay attention to yellow/amber letters
   - Try them in different positions

4. **Eliminate Strategically**
   - Gray letters are out - never use them again
   - Focus on remaining possibilities

5. **Think About Patterns**
   - Common endings: -ED, -ER, -LY, -ING
   - Common starts: ST-, CH-, TH-, PR-

### For Bot Analysis

1. **Test Multiple Strategies**
   - No single strategy wins every game
   - Compare results across 1000+ games for accuracy

2. **Understand Entropy**
   - Maximize Entropy finds optimal guesses
   - Balances information gain with risk

3. **Sample Size Matters**
   - Run 1000+ games for reliable statistics
   - 100 games can have high variance

---

## Keyboard Shortcuts

- **Enter**: Submit guess (in input field)
- **ESC**: Clear current input
- **Tab**: Navigate between controls

---

## Troubleshooting

### "Invalid word" Error
- Word not in dictionary
- Check spelling
- Try a common 5-letter word

### Login Issues
- Ensure username/email and password are correct
- Password must be at least 6 characters
- Contact admin if you forgot your password

### Game Not Starting
- Refresh the page
- Check browser console for errors
- Ensure JavaScript is enabled

### CSV Export Not Working
- Check browser download settings
- Try a different browser
- Ensure popup blockers aren't interfering

---

## Data & Privacy

### Guest Users
- No data is stored or tracked
- Games exist only in current session

### Registered Users
- Account information stored securely
- Passwords encrypted with BCrypt
- Game history tracking (future feature)
- No data shared with third parties

### Local Data
- H2 database stored in `./data/wordai`
- For development/testing purposes
- Can be deleted to reset all users

---

## Advanced Features

### Dictionary Customization
Dictionaries can be configured in `application.properties`:
```properties
wordai.dictionary.4letter=path/to/4_letter_words.txt
wordai.dictionary.5letter=path/to/5_letter_words.txt
wordai.dictionary.6letter=path/to/6_letter_words.txt
wordai.dictionary.7letter=path/to/7_letter_words.txt
```

### Strategy Configuration
Fixed-first-word strategy can be customized by modifying the bot strategy class.

---

## Getting Help

- **Issues**: Check the console for error messages
- **Questions**: Refer to code comments and documentation
- **Bugs**: Report via GitHub issues
- **Features**: Suggestions welcome!

---

## Version Information

- **Current Version**: 1.0-SNAPSHOT
- **Java**: 21
- **Spring Boot**: 3.4.0
- **Database**: H2 (development)

---

## Credits

WordAI is inspired by the popular Wordle game, enhanced with AI analysis and educational tools for studying word-guessing strategies.

---

**Happy Word Guessing! 🎮📊**
