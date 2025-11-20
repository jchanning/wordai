const API_BASE = '/api/wordai';
let currentGameId = null;
let gameEnded = false;
let currentWordLength = 5; // Track the current game's word length
let currentDictionarySize = 2315; // Track the current dictionary's total word count
let helpUsedCount = 0; // Track how many times help has been used in this game
const MAX_HELP_COUNT = 3; // Maximum number of help requests allowed per game
let currentGameGuesses = []; // Track all guesses with their responses and metrics for current game

// Initialize page
window.addEventListener('DOMContentLoaded', function() {
    newGame();
    updateHistoryDisplay();
    updateStats();
    updateHelpCounter();
});

function setupLetterInputs() {
    const inputs = document.querySelectorAll('.letter-input');
    
    inputs.forEach((input, index) => {
        // Handle typing in each input
        input.addEventListener('input', function(e) {
            const value = e.target.value.toUpperCase();
            e.target.value = value;
            
            // Add visual feedback for filled inputs
            if (value) {
                e.target.classList.add('filled');
                // Auto-focus next input
                if (index < inputs.length - 1) {
                    inputs[index + 1].focus();
                }
            } else {
                e.target.classList.remove('filled');
            }
        });

        // Handle backspace to move to previous input
        input.addEventListener('keydown', function(e) {
            if (e.key === 'Backspace' && !e.target.value && index > 0) {
                inputs[index - 1].focus();
                inputs[index - 1].value = '';
                inputs[index - 1].classList.remove('filled');
            }
            
            // Handle Enter key to submit guess
            if (e.key === 'Enter' && !gameEnded) {
                makeGuess();
            }
        });

        // Prevent non-letter characters
        input.addEventListener('keypress', function(e) {
            const char = String.fromCharCode(e.which);
            if (!/[a-zA-Z]/.test(char)) {
                e.preventDefault();
            }
        });
    });
}

function getCurrentGuess() {
    const inputs = document.querySelectorAll('.letter-input');
    let word = '';
    inputs.forEach(input => {
        word += input.value.toUpperCase();
    });
    return word;
}

function clearLetterInputs() {
    const inputs = document.querySelectorAll('.letter-input');
    inputs.forEach(input => {
        input.value = '';
        input.classList.remove('filled');
        input.disabled = false;
    });
    if (inputs.length > 0) {
        inputs[0].focus();
    }
}

function disableLetterInputs() {
    const inputs = document.querySelectorAll('.letter-input');
    inputs.forEach(input => {
        input.disabled = true;
    });
}

function showStatus(message, type = 'info') {
    const statusDiv = document.getElementById('status');
    statusDiv.textContent = message;
    statusDiv.className = `status ${type}`;
    statusDiv.style.display = 'block';
}

function hideStatus() {
    document.getElementById('status').style.display = 'none';
}

function updateHelpCounter() {
    const counterElement = document.getElementById('helpCounter');
    const button = document.getElementById('getSuggestionBtn');
    const remainingHelp = MAX_HELP_COUNT - helpUsedCount;
    
    counterElement.textContent = `${remainingHelp}/${MAX_HELP_COUNT}`;
    
    if (helpUsedCount >= MAX_HELP_COUNT) {
        button.disabled = true;
        button.style.backgroundColor = '#cccccc';
        button.style.cursor = 'not-allowed';
    } else {
        button.disabled = false;
        button.style.backgroundColor = '';
        button.style.cursor = 'pointer';
    }
}

async function newGame() {
    try {
        hideStatus();
        
        // Delete the previous game session if one exists
        if (currentGameId) {
            try {
                await fetch(`${API_BASE}/games/${currentGameId}`, {
                    method: 'DELETE'
                });
                console.log('Deleted previous game session:', currentGameId);
            } catch (deleteError) {
                console.warn('Failed to delete previous session:', deleteError);
                // Continue anyway - don't block new game creation
            }
        }
        
        // Get the selected dictionary ID
        const selector = document.getElementById('dictionarySelector');
        const dictionaryId = selector.value || null;
        
        const requestBody = dictionaryId ? { dictionaryId: dictionaryId } : {};
        
        const response = await fetch(`${API_BASE}/games`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(requestBody)
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();
        currentGameId = data.gameId;
        currentWordLength = data.wordLength; // Store the word length for this game
        gameEnded = false;
        
        document.getElementById('attempts').textContent = '0';
        document.getElementById('maxAttempts').textContent = data.maxAttempts;
        document.getElementById('guessHistory').innerHTML = '';
        clearLetterInputs();
        document.getElementById('guessBtn').disabled = false;
        
        // Initialize analytics panel with initial dictionary metrics
        if (data.dictionaryMetrics) {
            initializeAnalytics(data.dictionaryMetrics);
        } else {
            resetAnalytics();
        }
        
        // Update assistant panel
        updateAssistant('new');
        
        // Reset help counter for new game
        helpUsedCount = 0;
        updateHelpCounter();
        
        // Reset current game guesses
        currentGameGuesses = [];
        
        // Set the strategy for the new game based on dropdown selection
        await changeStrategy();
        
        showStatus('New game started! Start guessing!', 'success');
    } catch (error) {
        showStatus('Failed to create new game: ' + error.message, 'error');
    }
}

async function newSession() {
    // Clear session storage
    sessionStorage.removeItem('gameHistory');
    sessionStorage.removeItem('gameStats');
    
    // Update displays
    updateHistoryDisplay();
    updateStats();
    
    // Reset help counter
    helpUsedCount = 0;
    updateHelpCounter();
    
    // Start a new game
    await newGame();
    
    showStatus('New session started! Stats have been reset.', 'success');
}

async function makeGuess() {
    if (!currentGameId || gameEnded) {
        return;
    }

    const word = getCurrentGuess().trim();
    if (!word) {
        showStatus('Please enter a word!', 'error');
        return;
    }

    if (word.length !== currentWordLength) {
        showStatus(`Word must be ${currentWordLength} letters long!`, 'error');
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/games/${currentGameId}/guess`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ word: word })
        });

        const data = await response.json();
        
        if (!response.ok) {
            throw new Error(data.message || `HTTP error! status: ${response.status}`);
        }

        // Update game info
        document.getElementById('attempts').textContent = data.attemptNumber;
        
        // Update analytics if remaining words count is available
        if (data.remainingWordsCount !== undefined) {
            updateAnalytics(word, data.remainingWordsCount, data.dictionaryMetrics);
        }
        
        // Store detailed guess data for session analytics
        currentGameGuesses.push({
            attempt: data.attemptNumber,
            guess: word,
            results: data.results,
            remainingWords: data.remainingWordsCount,
            dictionaryMetrics: data.dictionaryMetrics
        });
        
        // Add guess to history
        addGuessToHistory(word, data.results);
        
        // Clear inputs
        clearLetterInputs();
        
        // Check game status
        if (data.gameWon) {
            gameEnded = true;
            disableLetterInputs();
            document.getElementById('guessBtn').disabled = true;
            showStatus('🎉 Congratulations! You won!', 'success');
            updateAssistant('won', data.attemptNumber);
            // Save game result
            saveGameResult(word, data.attemptNumber, true);
        } else if (data.gameOver) {
            gameEnded = true;
            disableLetterInputs();
            document.getElementById('guessBtn').disabled = true;
            showStatus('💔 Game Over! Better luck next time!', 'error');
            updateAssistant('lost', data.attemptNumber);
            // Fetch game status to get target word
            fetchTargetWordAndSave(data.attemptNumber);
        } else {
            showStatus(`Keep going! ${data.maxAttempts - data.attemptNumber} attempts left.`, 'info');
            updateAssistant('playing', data.attemptNumber, data.remainingWordsCount);
        }

    } catch (error) {
        showStatus('Error making guess: ' + error.message, 'error');
    }
}

function addGuessToHistory(word, results) {
    const historyDiv = document.getElementById('guessHistory');
    const guessRow = document.createElement('div');
    guessRow.className = 'guess-row';

    for (let i = 0; i < results.length; i++) {
        const letterBox = document.createElement('div');
        letterBox.className = 'letter-box';
        letterBox.textContent = results[i].letter;
        
        switch (results[i].status) {
            case 'G':
                letterBox.classList.add('letter-correct');
                break;
            case 'A':
                letterBox.classList.add('letter-present');
                break;
            case 'R':
            case 'X':  // eXcess letters display the same as Red (absent)
                letterBox.classList.add('letter-absent');
                break;
        }
        
        guessRow.appendChild(letterBox);
    }

    historyDiv.appendChild(guessRow);
}

async function checkHealth() {
    try {
        const response = await fetch(`${API_BASE}/health`);
        const data = await response.json();
        
        if (response.ok) {
            showStatus(`Server is running! Active sessions: ${data.activeSessions}`, 'success');
        } else {
            showStatus('Server health check failed', 'error');
        }
    } catch (error) {
        showStatus('Cannot connect to server: ' + error.message, 'error');
    }
}

function initializeAnalytics(dictionaryMetrics) {
    // Get total words from the server response
    const totalWords = dictionaryMetrics.totalWords || 2315;
    currentDictionarySize = totalWords; // Store for later use
    
    document.getElementById('totalWords').textContent = totalWords;
    document.getElementById('remainingWords').textContent = totalWords;
    document.getElementById('eliminatedWords').textContent = '0';
    document.getElementById('reductionPercent').textContent = '0%';
    
    // Populate dictionary metrics from server response
    document.getElementById('uniqueLetters').textContent = dictionaryMetrics.uniqueCharacters || '26';
    document.getElementById('letterCount').textContent = dictionaryMetrics.letterCount || '-';
    
    // Update column lengths bar chart with initial data
    if (dictionaryMetrics.columnLengths && dictionaryMetrics.columnLengths.length > 0) {
        updateColumnLengthsChart(dictionaryMetrics.columnLengths);
    } else {
        updateColumnLengthsChart(null);
    }
    
    document.getElementById('lastGuessInfo').textContent = 'Start guessing to see word reduction analytics!';
}

function resetAnalytics() {
    const totalWords = currentDictionarySize; // Use current dictionary size
    document.getElementById('totalWords').textContent = totalWords;
    document.getElementById('remainingWords').textContent = totalWords;
    document.getElementById('eliminatedWords').textContent = '0';
    document.getElementById('reductionPercent').textContent = '0%';
    document.getElementById('uniqueLetters').textContent = '26';
    document.getElementById('letterCount').textContent = '-';
    updateColumnLengthsChart(null);
    document.getElementById('lastGuessInfo').textContent = 'Start guessing to see word reduction analytics!';
}

function updateAnalytics(guessedWord, remainingCount, dictionaryMetrics) {
    const totalWords = currentDictionarySize;
    const eliminated = totalWords - remainingCount;
    const reductionPercent = ((eliminated / totalWords) * 100).toFixed(2);
    
    document.getElementById('remainingWords').textContent = remainingCount;
    document.getElementById('eliminatedWords').textContent = eliminated;
    document.getElementById('reductionPercent').textContent = reductionPercent + '%';
    document.getElementById('lastGuessInfo').textContent = 
        `After "${guessedWord}": ${remainingCount} words remain (${eliminated} eliminated)`;
    
    // Update dictionary metrics if available
    if (dictionaryMetrics) {
        document.getElementById('uniqueLetters').textContent = dictionaryMetrics.uniqueCharacters || '-';
        document.getElementById('letterCount').textContent = dictionaryMetrics.letterCount || '-';
        
        // Update column lengths bar chart
        if (dictionaryMetrics.columnLengths && dictionaryMetrics.columnLengths.length > 0) {
            updateColumnLengthsChart(dictionaryMetrics.columnLengths);
        } else {
            updateColumnLengthsChart(null);
        }
    }
}

function updateColumnLengthsChart(columnLengths) {
    const chartContainer = document.getElementById('columnLengthsChart');
    chartContainer.innerHTML = '';
    
    if (!columnLengths || columnLengths.length === 0) {
        chartContainer.innerHTML = '<div style="width: 100%; text-align: center; color: #888; align-self: center;">No data yet</div>';
        return;
    }
    
    const maxHeight = 26; // Maximum possible unique letters per position
    const chartHeight = 100; // Height of chart area in pixels
    
    columnLengths.forEach((count, index) => {
        const barContainer = document.createElement('div');
        barContainer.style.cssText = 'flex: 1; display: flex; flex-direction: column; align-items: center; gap: 4px;';
        
        // Create bar wrapper for positioning
        const barWrapper = document.createElement('div');
        barWrapper.style.cssText = `height: ${chartHeight}px; display: flex; align-items: flex-end; justify-content: center; width: 100%;`;
        
        // Calculate bar height as percentage of max (26)
        const heightPercent = (count / maxHeight) * 100;
        const barHeight = (heightPercent / 100) * chartHeight;
        
        // Create the bar
        const bar = document.createElement('div');
        bar.style.cssText = `
            width: 100%;
            height: ${barHeight}px;
            background: linear-gradient(180deg, var(--accent-blue-light) 0%, var(--accent-blue) 100%);
            border-radius: 4px 4px 0 0;
            transition: all 0.3s ease;
            box-shadow: 0 2px 8px rgba(33, 150, 243, 0.3);
            position: relative;
        `;
        
        // Add count label on top of bar
        const countLabel = document.createElement('div');
        countLabel.style.cssText = `
            position: absolute;
            top: -20px;
            left: 50%;
            transform: translateX(-50%);
            font-size: 0.75em;
            font-weight: 700;
            color: var(--primary-slate);
            white-space: nowrap;
        `;
        countLabel.textContent = count;
        bar.appendChild(countLabel);
        
        barWrapper.appendChild(bar);
        barContainer.appendChild(barWrapper);
        
        // Add position label below bar
        const label = document.createElement('div');
        label.style.cssText = 'font-size: 0.7em; color: #666; font-weight: bold;';
        label.textContent = `P${index + 1}`;
        barContainer.appendChild(label);
        
        chartContainer.appendChild(barContainer);
    });
}

function updateAssistant(status, attempts = 0, remainingWords = 0) {
    const statsDiv = document.getElementById('assistant-stats');
    
    if (status === 'new') {
        statsDiv.innerHTML = `
            <div style="color: #2e7d32; font-weight: bold;">📝 New game started!</div>
            <div style="margin-top: 5px; font-size: 0.85em;">Attempts: 0/6</div>
            <div style="font-size: 0.85em;">Good luck!</div>
        `;
    } else if (status === 'playing') {
        const maxAttempts = document.getElementById('maxAttempts').textContent;
        const remaining = parseInt(maxAttempts) - attempts;
        statsDiv.innerHTML = `
            <div style="font-weight: bold;">📝 Attempt ${attempts}/${maxAttempts}</div>
            <div style="margin-top: 5px; font-size: 0.85em;">${remaining} attempts remaining</div>
            <div style="font-size: 0.85em; color: #1976d2;">${remainingWords} possible words</div>
        `;
    } else if (status === 'won') {
        statsDiv.innerHTML = `
            <div style="color: #2e7d32; font-weight: bold;">🎉 Victory!</div>
            <div style="margin-top: 5px; font-size: 0.85em;">Solved in ${attempts} attempt${attempts > 1 ? 's' : ''}!</div>
            <div style="font-size: 0.85em;">Great job!</div>
        `;
    } else if (status === 'lost') {
        statsDiv.innerHTML = `
            <div style="color: #c62828; font-weight: bold;">💔 Game Over</div>
            <div style="margin-top: 5px; font-size: 0.85em;">Used all ${attempts} attempts</div>
            <div style="font-size: 0.85em;">Try again!</div>
        `;
    }
}

// Game History Management
async function fetchTargetWordAndSave(attempts) {
    try {
        const response = await fetch(`${API_BASE}/games/${currentGameId}`);
        const data = await response.json();
        // Use a placeholder if target word is not available
        const targetWord = data.targetWord || '?????';
        saveGameResult(targetWord, attempts, false);
    } catch (error) {
        console.error('Failed to fetch target word:', error);
        // Still save the game as lost even if we can't get the target word
        saveGameResult('?????', attempts, false);
    }
}

function getGameHistory() {
    const history = sessionStorage.getItem('gameHistory');
    return history ? JSON.parse(history) : [];
}

function getGameStats() {
    const stats = sessionStorage.getItem('gameStats');
    return stats ? JSON.parse(stats) : { total: 0, won: 0, lost: 0, wonGames: [] };
}

function saveGameResult(targetWord, attempts, won) {
    // Save to limited history (for display)
    const history = getGameHistory();
    history.unshift({
        targetWord: targetWord,
        attempts: attempts,
        won: won,
        timestamp: new Date().toISOString(),
        guesses: [...currentGameGuesses], // Store all guesses with detailed metrics
        wordLength: currentWordLength,
        dictionarySize: currentDictionarySize
    });
    
    // Keep only last 10 games in history (for display)
    if (history.length > 10) {
        history.length = 10;
    }
    sessionStorage.setItem('gameHistory', JSON.stringify(history));
    
    // Update cumulative stats (not limited)
    const stats = getGameStats();
    stats.total++;
    if (won) {
        stats.won++;
        stats.wonGames.push(attempts);
    } else {
        stats.lost++;
    }
    sessionStorage.setItem('gameStats', JSON.stringify(stats));
    
    updateHistoryDisplay();
    updateStats();
}

function updateHistoryDisplay() {
    const history = getGameHistory();
    const historyContainer = document.getElementById('game-history');
    
    if (history.length === 0) {
        historyContainer.innerHTML = '<p style="text-align: center; color: #888; padding: 20px;">No games played yet</p>';
        return;
    }
    
    // Display only last 5 games
    const recentGames = history.slice(0, 5);
    historyContainer.innerHTML = recentGames.map(game => `
        <div class="history-item">
            <div class="history-word">${game.targetWord.toUpperCase()}</div>
            <div class="history-attempts">${game.attempts} attempt${game.attempts !== 1 ? 's' : ''}</div>
            <span class="history-status ${game.won ? 'history-won' : 'history-lost'}">
                ${game.won ? '✓ WON' : '✗ LOST'}
            </span>
        </div>
    `).join('');
}

function updateStats() {
    const stats = getGameStats();
    
    if (stats.total === 0) {
        document.getElementById('stat-total').textContent = '0';
        document.getElementById('stat-won').textContent = '0';
        document.getElementById('stat-lost').textContent = '0';
        document.getElementById('stat-winrate').textContent = '0%';
        document.getElementById('stat-min').textContent = '-';
        document.getElementById('stat-max').textContent = '-';
        document.getElementById('stat-avg').textContent = '-';
        return;
    }
    
    const total = stats.total;
    const won = stats.won;
    const lost = stats.lost;
    const winRate = Math.round((won / total) * 100);
    
    // Calculate min, max, avg from won games
    let minGuesses = '-';
    let maxGuesses = '-';
    let avgGuesses = '-';
    
    if (stats.wonGames.length > 0) {
        minGuesses = Math.min(...stats.wonGames);
        maxGuesses = Math.max(...stats.wonGames);
        const sum = stats.wonGames.reduce((a, b) => a + b, 0);
        avgGuesses = (sum / stats.wonGames.length).toFixed(1);
    }
    
    document.getElementById('stat-total').textContent = total;
    document.getElementById('stat-won').textContent = won;
    document.getElementById('stat-lost').textContent = lost;
    document.getElementById('stat-winrate').textContent = winRate + '%';
    document.getElementById('stat-min').textContent = minGuesses;
    document.getElementById('stat-max').textContent = maxGuesses;
    document.getElementById('stat-avg').textContent = avgGuesses;
}

// Strategy and Suggestion Functions
async function changeStrategy() {
    if (!currentGameId) {
        showStatus('No active game', 'error');
        return;
    }

    const strategy = document.getElementById('strategySelector').value;
    
    try {
        const response = await fetch(`${API_BASE}/games/${currentGameId}/strategy`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ strategy: strategy })
        });

        if (response.ok) {
            updateStrategyTips(strategy);
            showStatus(`Strategy changed to ${strategy}`, 'success');
        } else {
            showStatus('Failed to change strategy', 'error');
        }
    } catch (error) {
        showStatus('Error changing strategy: ' + error.message, 'error');
    }
}

async function getSuggestion() {
    if (!currentGameId) {
        showStatus('No active game', 'error');
        return;
    }

    if (gameEnded) {
        showStatus('Game has ended', 'error');
        return;
    }

    // Check if help limit reached
    if (helpUsedCount >= MAX_HELP_COUNT) {
        showStatus('Maximum help requests reached (3/3)', 'error');
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/games/${currentGameId}/suggestion`);
        const data = await response.json();

        if (response.ok && data.suggestion) {
            // Increment help counter and update display
            helpUsedCount++;
            updateHelpCounter();
            
            document.getElementById('suggestedWord').textContent = data.suggestion.toUpperCase();
            showStatus(`Suggestion: ${data.suggestion.toUpperCase()} (${data.strategy})`, 'success');
        } else if (response.ok && !data.suggestion) {
            document.getElementById('suggestedWord').textContent = 'No words available';
            showStatus('No valid words remaining', 'error');
        } else {
            showStatus('Failed to get suggestion', 'error');
        }
    } catch (error) {
        showStatus('Error getting suggestion: ' + error.message, 'error');
    }
}

function updateStrategyTips(strategy) {
    const tipsDiv = document.getElementById('strategyTips');
    
    if (strategy === 'RANDOM') {
        tipsDiv.innerHTML = '<strong>Random:</strong> Selects words randomly from valid options. Simple but effective!';
    } else if (strategy === 'ENTROPY') {
        tipsDiv.innerHTML = '<strong>Maximum Entropy:</strong> Chooses words that maximize information gain. Uses statistical analysis to find the most informative guess.';
    } else if (strategy === 'MOST_COMMON_LETTERS') {
        tipsDiv.innerHTML = '<strong>Most Common Letters:</strong> Selects words containing the most frequently occurring letters in the remaining word pool. Prioritizes common letter patterns.';
    }
}

// Dictionary selection functions
let availableDictionaries = [];

async function loadDictionaries() {
    try {
        const response = await fetch(`${API_BASE}/dictionaries`);
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        availableDictionaries = await response.json();
        populateDictionarySelector();
    } catch (error) {
        console.error('Failed to load dictionaries:', error);
        showStatus('Failed to load dictionaries: ' + error.message, 'error');
        
        // Fallback to default option
        const selector = document.getElementById('dictionarySelector');
        selector.innerHTML = '<option value="">Standard (5 Letters)</option>';
    }
}

function populateDictionarySelector() {
    const selector = document.getElementById('dictionarySelector');
    selector.innerHTML = '';

    availableDictionaries.forEach(dict => {
        const option = document.createElement('option');
        option.value = dict.id;
        option.textContent = dict.name;
        
        if (!dict.available) {
            option.disabled = true;
            option.textContent += ' (Not Available)';
        }
        
        if (dict.description) {
            option.title = dict.description;
        }
        
        selector.appendChild(option);
    });

    // Select the 5-letter dictionary by default, fallback to first available
    const fiveLetterDict = availableDictionaries.find(d => d.available && d.wordLength === 5);
    const defaultDict = fiveLetterDict || availableDictionaries.find(d => d.available);
    
    if (defaultDict) {
        selector.value = defaultDict.id;
        // Adjust the letter input grid for the selected dictionary's word length
        adjustLetterInputGrid(defaultDict.wordLength);
    }
}

function onDictionaryChange() {
    const selector = document.getElementById('dictionarySelector');
    const selectedId = selector.value;
    const selectedDict = availableDictionaries.find(d => d.id === selectedId);
    
    if (selectedDict) {
        showStatus(`Dictionary changed to: ${selectedDict.name} (${selectedDict.wordLength} letters)`, 'success');
        
        // Adjust the letter input grid for the new word length
        adjustLetterInputGrid(selectedDict.wordLength);
    }
}

function adjustLetterInputGrid(wordLength) {
    const letterInputs = document.getElementById('letterInputs');
    letterInputs.innerHTML = '';

    for (let i = 0; i < wordLength; i++) {
        const input = document.createElement('input');
        input.type = 'text';
        input.className = 'letter-input';
        input.maxLength = 1;
        input.id = `letter${i}`;
        
        // Add the same event handlers as before
        input.addEventListener('input', function(e) {
            this.value = this.value.toUpperCase();
            if (this.value && i < wordLength - 1) {
                document.getElementById(`letter${i + 1}`).focus();
            }
        });
        
        input.addEventListener('keydown', function(e) {
            if (e.key === 'Backspace' && !this.value && i > 0) {
                document.getElementById(`letter${i - 1}`).focus();
            } else if (e.key === 'Enter') {
                makeGuess();
            }
        });
        
        letterInputs.appendChild(input);
    }
    
    // Focus the first input
    if (wordLength > 0) {
        document.getElementById('letter0').focus();
    }
}

// Session Viewer Functions
function showSessionViewer() {
    document.getElementById('gameView').style.display = 'none';
    document.getElementById('sessionViewer').style.display = 'block';
    renderSessionDetails();
}

function hideSessionViewer() {
    document.getElementById('sessionViewer').style.display = 'none';
    document.getElementById('gameView').style.display = 'flex';
}

function renderSessionDetails() {
    const history = getGameHistory();
    const stats = getGameStats();
    const contentDiv = document.getElementById('sessionContent');

    if (history.length === 0) {
        contentDiv.innerHTML = `
            <div style="text-align: center; padding: 40px; color: #888;">
                <h3>No games played yet</h3>
                <p>Start playing to see detailed session analytics!</p>
            </div>
        `;
        return;
    }

    // Build comprehensive analytics view
    let html = `
        <!-- Overall Session Statistics -->
        <div style="background: rgba(33, 150, 243, 0.08); padding: 20px; border-radius: 12px; margin-bottom: 20px; border: 2px solid var(--accent-blue-light);">
            <h3 style="margin-top: 0; color: var(--primary-slate);">📊 Overall Session Statistics</h3>
            <div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 15px; margin-top: 15px;">
                <div style="background: white; padding: 15px; border-radius: 8px; text-align: center; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                    <div style="font-size: 0.8em; color: #666; font-weight: 700; text-transform: uppercase; margin-bottom: 8px;">Total Games</div>
                    <div style="font-size: 2em; font-weight: 800; color: var(--accent-blue); font-family: var(--font-mono);">${stats.total}</div>
                </div>
                <div style="background: white; padding: 15px; border-radius: 8px; text-align: center; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                    <div style="font-size: 0.8em; color: #666; font-weight: 700; text-transform: uppercase; margin-bottom: 8px;">Win Rate</div>
                    <div style="font-size: 2em; font-weight: 800; color: var(--success-green); font-family: var(--font-mono);">${stats.total > 0 ? Math.round((stats.won / stats.total) * 100) : 0}%</div>
                </div>
                <div style="background: white; padding: 15px; border-radius: 8px; text-align: center; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                    <div style="font-size: 0.8em; color: #666; font-weight: 700; text-transform: uppercase; margin-bottom: 8px;">Wins</div>
                    <div style="font-size: 2em; font-weight: 800; color: var(--success-green); font-family: var(--font-mono);">${stats.won}</div>
                </div>
                <div style="background: white; padding: 15px; border-radius: 8px; text-align: center; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                    <div style="font-size: 0.8em; color: #666; font-weight: 700; text-transform: uppercase; margin-bottom: 8px;">Losses</div>
                    <div style="font-size: 2em; font-weight: 800; color: var(--error-red); font-family: var(--font-mono);">${stats.lost}</div>
                </div>
            </div>
    `;

    // Add performance metrics if we have won games
    if (stats.wonGames.length > 0) {
        const minAttempts = Math.min(...stats.wonGames);
        const maxAttempts = Math.max(...stats.wonGames);
        const avgAttempts = (stats.wonGames.reduce((a, b) => a + b, 0) / stats.wonGames.length).toFixed(2);
        
        html += `
            <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 15px; margin-top: 15px;">
                <div style="background: white; padding: 15px; border-radius: 8px; text-align: center; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                    <div style="font-size: 0.8em; color: #666; font-weight: 700; text-transform: uppercase; margin-bottom: 8px;">Best (Min)</div>
                    <div style="font-size: 2em; font-weight: 800; color: var(--success-green); font-family: var(--font-mono);">${minAttempts}</div>
                </div>
                <div style="background: white; padding: 15px; border-radius: 8px; text-align: center; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                    <div style="font-size: 0.8em; color: #666; font-weight: 700; text-transform: uppercase; margin-bottom: 8px;">Average</div>
                    <div style="font-size: 2em; font-weight: 800; color: var(--accent-blue); font-family: var(--font-mono);">${avgAttempts}</div>
                </div>
                <div style="background: white; padding: 15px; border-radius: 8px; text-align: center; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                    <div style="font-size: 0.8em; color: #666; font-weight: 700; text-transform: uppercase; margin-bottom: 8px;">Worst (Max)</div>
                    <div style="font-size: 2em; font-weight: 800; color: var(--warning-amber); font-family: var(--font-mono);">${maxAttempts}</div>
                </div>
            </div>
        `;
    }

    html += `</div>`;

    // Detailed game-by-game breakdown
    html += `
        <h3 style="color: var(--primary-slate); margin-top: 30px; margin-bottom: 15px;">🎮 Game Details</h3>
    `;

    history.forEach((game, index) => {
        const gameNumber = history.length - index;
        const date = new Date(game.timestamp);
        const formattedDate = date.toLocaleString();

        html += `
            <div style="background: white; border-radius: 12px; padding: 20px; margin-bottom: 20px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); border-left: 5px solid ${game.won ? 'var(--success-green)' : 'var(--error-red)'};">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px;">
                    <div>
                        <h4 style="margin: 0; color: var(--primary-slate);">Game #${gameNumber}: ${game.targetWord.toUpperCase()}</h4>
                        <div style="font-size: 0.85em; color: #888; margin-top: 5px;">${formattedDate}</div>
                    </div>
                    <div style="text-align: right;">
                        <span style="display: inline-block; padding: 8px 16px; border-radius: 20px; font-weight: 700; font-size: 0.9em; ${game.won ? 'background: var(--success-green); color: white;' : 'background: var(--error-red); color: white;'}">
                            ${game.won ? '✓ WON' : '✗ LOST'}
                        </span>
                        <div style="margin-top: 8px; font-size: 1.2em; font-weight: 700; color: var(--primary-slate);">${game.attempts} Attempts</div>
                    </div>
                </div>
        `;

        // Show detailed guess history if available
        if (game.guesses && game.guesses.length > 0) {
            html += `
                <div style="margin-top: 20px;">
                    <h5 style="color: var(--primary-slate); margin-bottom: 10px;">Guess History:</h5>
                    <table style="width: 100%; border-collapse: collapse;">
                        <thead>
                            <tr style="background: var(--gray-100); font-size: 0.85em; text-transform: uppercase;">
                                <th style="padding: 10px; text-align: left; border-bottom: 2px solid var(--gray-300);">Attempt</th>
                                <th style="padding: 10px; text-align: left; border-bottom: 2px solid var(--gray-300);">Guess</th>
                                <th style="padding: 10px; text-align: left; border-bottom: 2px solid var(--gray-300);">Response</th>
                                <th style="padding: 10px; text-align: right; border-bottom: 2px solid var(--gray-300);">Remaining Words</th>
                                <th style="padding: 10px; text-align: right; border-bottom: 2px solid var(--gray-300);">Reduction %</th>
                            </tr>
                        </thead>
                        <tbody>
            `;

            game.guesses.forEach((guessData) => {
                const reductionPercent = game.dictionarySize > 0 
                    ? (((game.dictionarySize - guessData.remainingWords) / game.dictionarySize) * 100).toFixed(1)
                    : 0;

                // Build visual response
                let responseHtml = '<div style="display: flex; gap: 3px;">';
                guessData.results.forEach(result => {
                    let bgColor = '#787c7e'; // Red/absent
                    if (result.status === 'G') bgColor = '#6aaa64'; // Green
                    else if (result.status === 'A') bgColor = '#c9b458'; // Amber
                    
                    responseHtml += `<div style="width: 30px; height: 30px; background: ${bgColor}; color: white; display: flex; align-items: center; justify-content: center; border-radius: 4px; font-weight: 700; font-family: var(--font-mono);">${result.letter}</div>`;
                });
                responseHtml += '</div>';

                html += `
                    <tr style="border-bottom: 1px solid var(--gray-200);">
                        <td style="padding: 12px; font-weight: 700; color: var(--primary-slate);">#${guessData.attempt}</td>
                        <td style="padding: 12px; font-family: var(--font-mono); font-weight: 700; font-size: 1.1em;">${guessData.guess.toUpperCase()}</td>
                        <td style="padding: 12px;">${responseHtml}</td>
                        <td style="padding: 12px; text-align: right; font-family: var(--font-mono); font-weight: 600;">${guessData.remainingWords || 'N/A'}</td>
                        <td style="padding: 12px; text-align: right; font-family: var(--font-mono); font-weight: 600; color: var(--success-green);">${reductionPercent}%</td>
                    </tr>
                `;

                // Add dictionary metrics if available
                if (guessData.dictionaryMetrics) {
                    html += `
                        <tr style="background: rgba(33, 150, 243, 0.05);">
                            <td colspan="5" style="padding: 10px 12px; font-size: 0.85em;">
                                <strong>Metrics:</strong> 
                                Unique Letters: ${guessData.dictionaryMetrics.uniqueCharacters || 'N/A'} | 
                                Total Letter Count: ${guessData.dictionaryMetrics.letterCount || 'N/A'}
                                ${guessData.dictionaryMetrics.columnLengths ? ' | Column Lengths: [' + guessData.dictionaryMetrics.columnLengths.join(', ') + ']' : ''}
                            </td>
                        </tr>
                    `;
                }
            });

            html += `
                        </tbody>
                    </table>
                </div>
            `;
        } else {
            html += `<div style="color: #888; font-style: italic; margin-top: 10px;">Detailed guess data not available for this game.</div>`;
        }

        html += `</div>`;
    });

    contentDiv.innerHTML = html;
}

// Initialize dictionaries on page load
loadDictionaries();