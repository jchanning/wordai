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
        
        // Create letter inputs based on word length
        adjustLetterInputGrid(data.wordLength);
        
        document.getElementById('attempts').textContent = '0';
        document.getElementById('maxAttempts').textContent = data.maxAttempts;
        document.getElementById('guessHistory').innerHTML = '';
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
        
        // Clear suggested word from previous game
        document.getElementById('suggestedWord').textContent = '';
        
        // Reset dictionary viewer (after analytics are initialized)
        const dictionaryViewer = document.getElementById('dictionaryViewer');
        if (dictionaryViewer) {
            dictionaryViewer.style.display = 'none';
            document.getElementById('dictionaryWordList').innerHTML = '';
        }
        const viewDictionaryBtn = document.getElementById('viewDictionaryBtn');
        if (viewDictionaryBtn) {
            const wordCountElement = document.getElementById('dictionaryWordCount');
            const count = wordCountElement ? wordCountElement.textContent : '0';
            viewDictionaryBtn.innerHTML = `View Words (<span id="dictionaryWordCount">${count}</span>)`;
        }
        
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
        
        // Refresh dictionary viewer if it's currently visible
        const dictionaryViewer = document.getElementById('dictionaryViewer');
        if (dictionaryViewer && dictionaryViewer.style.display !== 'none') {
            await loadDictionaryWords();
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
            // Fetch game status to get target word and update status
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
    
    // Initialize dictionary word count for viewer
    const wordCountElement = document.getElementById('dictionaryWordCount');
    if (wordCountElement) {
        wordCountElement.textContent = totalWords;
    }
    
    // Populate dictionary metrics from server response
    document.getElementById('uniqueLetters').textContent = dictionaryMetrics.uniqueCharacters || '26';
    document.getElementById('letterCount').textContent = dictionaryMetrics.letterCount || '-';
    
    // Update column lengths bar chart with initial data
    if (dictionaryMetrics.columnLengths && dictionaryMetrics.columnLengths.length > 0) {
        updateColumnLengthsChart(dictionaryMetrics.columnLengths);
    } else {
        updateColumnLengthsChart(null);
    }
    
    // Update occurrence count by position table
    if (dictionaryMetrics.occurrenceCountByPosition) {
        updateOccurrenceTable(dictionaryMetrics.occurrenceCountByPosition);
    } else {
        updateOccurrenceTable(null);
    }
    
    // Update most frequent char by position table
    if (dictionaryMetrics.mostFrequentCharByPosition) {
        updateMostFrequentTable(dictionaryMetrics.mostFrequentCharByPosition);
    } else {
        updateMostFrequentTable(null);
    }
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
    updateOccurrenceTable(null);
    updateMostFrequentTable(null);
}

function updateAnalytics(guessedWord, remainingCount, dictionaryMetrics) {
    const totalWords = currentDictionarySize;
    const eliminated = totalWords - remainingCount;
    const reductionPercent = ((eliminated / totalWords) * 100).toFixed(2);
    
    document.getElementById('remainingWords').textContent = remainingCount;
    document.getElementById('eliminatedWords').textContent = eliminated;
    document.getElementById('reductionPercent').textContent = reductionPercent + '%';
    
    // Update dictionary word count for viewer (if element exists)
    const wordCountElement = document.getElementById('dictionaryWordCount');
    if (wordCountElement) {
        wordCountElement.textContent = remainingCount;
    }
    
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
        
        // Update occurrence count by position table
        if (dictionaryMetrics.occurrenceCountByPosition) {
            updateOccurrenceTable(dictionaryMetrics.occurrenceCountByPosition);
        }
        
        // Update most frequent char by position table
        if (dictionaryMetrics.mostFrequentCharByPosition) {
            updateMostFrequentTable(dictionaryMetrics.mostFrequentCharByPosition);
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
    const chartHeight = 120; // Height available for bars (reduced to leave space for labels below)
    
    columnLengths.forEach((count, index) => {
        const barContainer = document.createElement('div');
        barContainer.style.cssText = 'flex: 1; display: flex; flex-direction: column; align-items: center; gap: 6px;';
        
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
            background: linear-gradient(180deg, var(--accent-primary) 0%, #1e40af 100%);
            border-radius: 4px 4px 0 0;
            transition: all 0.3s ease;
            box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
        `;
        
        barWrapper.appendChild(bar);
        barContainer.appendChild(barWrapper);
        
        // Add count label below bar
        const countLabel = document.createElement('div');
        countLabel.style.cssText = 'font-size: 0.85em; font-weight: 700; color: var(--text-primary);';
        countLabel.textContent = count;
        barContainer.appendChild(countLabel);
        
        // Add position label below count
        const positionLabel = document.createElement('div');
        positionLabel.style.cssText = 'font-size: 0.7em; color: var(--text-secondary); font-weight: bold;';
        positionLabel.textContent = `P${index + 1}`;
        barContainer.appendChild(positionLabel);
        
        chartContainer.appendChild(barContainer);
    });
}

function updateOccurrenceTable(occurrenceData) {
    const tableContainer = document.getElementById('occurrenceTable');
    tableContainer.innerHTML = '';
    
    if (!occurrenceData || Object.keys(occurrenceData).length === 0) {
        tableContainer.innerHTML = '<div style="text-align: center; color: #888; padding: 20px;">No data yet</div>';
        return;
    }
    
    // Create table
    const table = document.createElement('table');
    table.style.cssText = 'width: 100%; border-collapse: collapse; font-size: 0.85em; font-family: monospace; table-layout: fixed;';
    
    // Get number of positions from first entry
    const firstKey = Object.keys(occurrenceData)[0];
    const numPositions = occurrenceData[firstKey] ? occurrenceData[firstKey].length : 0;
    
    // Create header row
    const thead = document.createElement('thead');
    const headerRow = document.createElement('tr');
    
    // Letter column header - empty header with narrower width
    const letterHeader = document.createElement('th');
    letterHeader.textContent = '';
    letterHeader.style.cssText = 'padding: 6px 8px; text-align: center; border-bottom: 2px solid var(--text-secondary); position: sticky; top: 0; background: var(--bg-primary); z-index: 1; font-weight: 600; width: 30px;';
    headerRow.appendChild(letterHeader);
    
    // Position column headers
    for (let i = 0; i < numPositions; i++) {
        const posHeader = document.createElement('th');
        posHeader.textContent = `P${i + 1}`;
        posHeader.style.cssText = 'padding: 6px 4px; text-align: center; border-bottom: 2px solid var(--text-secondary); position: sticky; top: 0; background: var(--bg-primary); z-index: 1; font-weight: 600;';
        headerRow.appendChild(posHeader);
    }
    
    thead.appendChild(headerRow);
    table.appendChild(thead);
    
    // Create body
    const tbody = document.createElement('tbody');
    
    // Sort letters a-z
    const letters = Object.keys(occurrenceData).sort();
    
    letters.forEach((letter, idx) => {
        const row = document.createElement('tr');
        row.style.cssText = `border-bottom: 1px solid rgba(255,255,255,0.1); ${idx % 2 === 1 ? 'background: rgba(255,255,255,0.02);' : ''}`;
        
        // Letter cell - centered with reduced padding
        const letterCell = document.createElement('td');
        letterCell.textContent = letter.toUpperCase();
        letterCell.style.cssText = 'padding: 6px 4px; font-weight: bold; text-align: center; color: var(--text-primary);';
        row.appendChild(letterCell);
        
        // Position count cells
        const counts = occurrenceData[letter] || [];
        const maxCount = Math.max(...counts);
        
        for (let i = 0; i < numPositions; i++) {
            const countCell = document.createElement('td');
            const count = counts[i] || 0;
            countCell.textContent = count;
            
            // Highlight cells with higher counts
            let bgColor = '';
            if (count > 0 && maxCount > 0) {
                const intensity = count / maxCount;
                bgColor = `background: rgba(106, 170, 100, ${intensity * 0.3});`;
            }
            
            countCell.style.cssText = `padding: 6px 4px; text-align: center; ${count > 0 ? 'color: var(--text-primary); font-weight: 500;' : 'color: var(--text-secondary);'} ${bgColor}`;
            row.appendChild(countCell);
        }
        
        tbody.appendChild(row);
    });
    
    table.appendChild(tbody);
    tableContainer.appendChild(table);
}

function updateMostFrequentTable(mostFrequentData) {
    const tableContainer = document.getElementById('mostFrequentTable');
    tableContainer.innerHTML = '';
    
    if (!mostFrequentData || mostFrequentData.length === 0) {
        tableContainer.innerHTML = '<div style="text-align: center; color: #888; padding: 20px;">No data yet</div>';
        return;
    }
    
    // Create table
    const table = document.createElement('table');
    table.style.cssText = 'width: 100%; border-collapse: collapse; font-size: 0.9em; font-family: monospace;';
    
    // Create single row for data
    const row = document.createElement('tr');
    
    mostFrequentData.forEach((letter, index) => {
        const cell = document.createElement('td');
        cell.textContent = letter ? letter.toUpperCase() : '-';
        cell.style.cssText = `padding: 12px 8px; text-align: center; font-weight: bold; font-size: 1.2em; color: var(--correct-color); border: 1px solid rgba(255,255,255,0.1); background: rgba(106, 170, 100, 0.15);`;
        row.appendChild(cell);
    });
    
    table.appendChild(row);
    
    // Create position labels row
    const labelRow = document.createElement('tr');
    
    mostFrequentData.forEach((letter, index) => {
        const labelCell = document.createElement('td');
        labelCell.textContent = `P${index + 1}`;
        labelCell.style.cssText = 'padding: 6px 4px; text-align: center; font-size: 0.85em; color: var(--text-secondary); font-weight: 600; border: 1px solid rgba(255,255,255,0.1);';
        labelRow.appendChild(labelCell);
    });
    
    table.appendChild(labelRow);
    tableContainer.appendChild(table);
}

function updateAssistant(status, attempts = 0, remainingWords = 0) {
    // Quick Stats section has been removed
    // This function is now a no-op but kept for compatibility
    return;
}

// Game History Management
async function fetchTargetWordAndSave(attempts) {
    try {
        const response = await fetch(`${API_BASE}/games/${currentGameId}`);
        const data = await response.json();
        // Use a placeholder if target word is not available
        const targetWord = data.targetWord || '?????';
        showStatus(`Game Over! The word was: ${targetWord.toUpperCase()}`, 'error');
        saveGameResult(targetWord, attempts, false);
    } catch (error) {
        console.error('Failed to fetch target word:', error);
        showStatus('Game Over! Better luck next time!', 'error');
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
        historyContainer.innerHTML = '<p style="text-align: center; color: var(--text-secondary); padding: 20px;">No games played yet</p>';
        return;
    }
    
    // Display only last 5 games
    const recentGames = history.slice(0, 5);
    
    let tableHtml = `
        <table style="width: 100%; border-collapse: collapse; font-size: 0.9em;">
            <thead>
                <tr style="border-bottom: 1px solid var(--border-color); color: var(--text-secondary); text-align: left;">
                    <th style="padding: 8px;">Word</th>
                    <th style="padding: 8px;">Attempts</th>
                    <th style="padding: 8px; text-align: right;">Result</th>
                </tr>
            </thead>
            <tbody>
    `;
    
    recentGames.forEach(game => {
        tableHtml += `
            <tr style="border-bottom: 1px solid var(--border-color);">
                <td style="padding: 8px; font-family: var(--font-mono); font-weight: 700; color: var(--text-primary);">${game.targetWord.toUpperCase()}</td>
                <td style="padding: 8px; color: var(--text-secondary);">${game.attempts}</td>
                <td style="padding: 8px; text-align: right;">
                    <span style="color: ${game.won ? 'var(--success)' : 'var(--error)'}; font-weight: 700;">
                        ${game.won ? 'WON' : 'LOST'}
                    </span>
                </td>
            </tr>
        `;
    });
    
    tableHtml += `</tbody></table>`;
    historyContainer.innerHTML = tableHtml;
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
    const winRateValue = (won / total) * 100;
    const winRate = winRateValue === 100 ? '100' : winRateValue.toFixed(2);
    
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

function getStrategyDisplayName(strategy) {
    switch(strategy) {
        case 'RANDOM':
            return 'Random Selection';
        case 'ENTROPY':
            return 'Maximum Entropy';
        case 'MOST_COMMON_LETTERS':
            return 'Most Common Letters';
        case 'MINIMISE_COLUMN_LENGTHS':
            return 'Minimise Column Lengths';
        default:
            return strategy;
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
    } else if (strategy === 'MINIMISE_COLUMN_LENGTHS') {
        tipsDiv.innerHTML = '<strong>Minimise Column Lengths:</strong> Selects guesses that most effectively reduce the number of possible letters at each position. Directly minimizes positional entropy for maximum constraint.';
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
        showStatus(`Dictionary changed to: ${selectedDict.name}`, 'success');
        
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
    document.getElementById('gameView').style.display = 'grid';
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
        <div style="background: rgba(33, 150, 243, 0.08); padding: 20px; border-radius: 12px; margin-bottom: 20px; border: 2px solid var(--accent-primary);">
            <h3 style="margin-top: 0; color: var(--text-primary);">Overall Session Statistics</h3>
            <div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 15px; margin-top: 15px;">
                <div style="background: white; padding: 15px; border-radius: 8px; text-align: center; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                    <div style="font-size: 0.8em; color: var(--bg-tertiary); font-weight: 700; text-transform: uppercase; margin-bottom: 8px;">Total Games</div>
                    <div style="font-size: 2em; font-weight: 800; color: var(--accent-primary); font-family: var(--font-mono);">${stats.total}</div>
                </div>
                <div style="background: white; padding: 15px; border-radius: 8px; text-align: center; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                    <div style="font-size: 0.8em; color: var(--bg-tertiary); font-weight: 700; text-transform: uppercase; margin-bottom: 8px;">Win Rate</div>
                    <div style="font-size: 2em; font-weight: 800; color: var(--success); font-family: var(--font-mono);">${stats.total > 0 ? Math.round((stats.won / stats.total) * 100) : 0}%</div>
                </div>
                <div style="background: white; padding: 15px; border-radius: 8px; text-align: center; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                    <div style="font-size: 0.8em; color: var(--bg-tertiary); font-weight: 700; text-transform: uppercase; margin-bottom: 8px;">Wins</div>
                    <div style="font-size: 2em; font-weight: 800; color: var(--success); font-family: var(--font-mono);">${stats.won}</div>
                </div>
                <div style="background: white; padding: 15px; border-radius: 8px; text-align: center; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                    <div style="font-size: 0.8em; color: var(--bg-tertiary); font-weight: 700; text-transform: uppercase; margin-bottom: 8px;">Losses</div>
                    <div style="font-size: 2em; font-weight: 800; color: var(--error); font-family: var(--font-mono);">${stats.lost}</div>
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
                    <div style="font-size: 0.8em; color: var(--bg-tertiary); font-weight: 700; text-transform: uppercase; margin-bottom: 8px;">Best (Min)</div>
                    <div style="font-size: 2em; font-weight: 800; color: var(--success); font-family: var(--font-mono);">${minAttempts}</div>
                </div>
                <div style="background: white; padding: 15px; border-radius: 8px; text-align: center; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                    <div style="font-size: 0.8em; color: var(--bg-tertiary); font-weight: 700; text-transform: uppercase; margin-bottom: 8px;">Average</div>
                    <div style="font-size: 2em; font-weight: 800; color: var(--accent-primary); font-family: var(--font-mono);">${avgAttempts}</div>
                </div>
                <div style="background: white; padding: 15px; border-radius: 8px; text-align: center; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                    <div style="font-size: 0.8em; color: var(--bg-tertiary); font-weight: 700; text-transform: uppercase; margin-bottom: 8px;">Worst (Max)</div>
                    <div style="font-size: 2em; font-weight: 800; color: var(--warning); font-family: var(--font-mono);">${maxAttempts}</div>
                </div>
            </div>
        `;
    }

    html += `</div>`;

    // Detailed game-by-game breakdown
    html += `
        <h3 style="color: var(--text-primary); margin-top: 30px; margin-bottom: 15px;">Game Details</h3>
    `;

    history.forEach((game, index) => {
        const gameNumber = history.length - index;
        const date = new Date(game.timestamp);
        const formattedDate = date.toLocaleString();

        html += `
            <div style="background: white; color: var(--bg-primary); border-radius: 12px; padding: 20px; margin-bottom: 20px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); border-left: 5px solid ${game.won ? 'var(--success-green)' : 'var(--error-red)'};">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px;">
                    <div>
                        <h4 style="margin: 0; color: var(--bg-primary);">Game #${gameNumber}: ${game.targetWord.toUpperCase()}</h4>
                        <div style="font-size: 0.85em; color: #888; margin-top: 5px;">${formattedDate}</div>
                    </div>
                    <div style="text-align: right;">
                        <span style="display: inline-block; padding: 8px 16px; border-radius: 20px; font-weight: 700; font-size: 0.9em; ${game.won ? 'background: var(--success-green); color: white;' : 'background: var(--error-red); color: white;'}">
                            ${game.won ? '✓ WON' : '✗ LOST'}
                        </span>
                        <div style="margin-top: 8px; font-size: 1.2em; font-weight: 700; color: var(--bg-primary);">${game.attempts} Attempts</div>
                    </div>
                </div>
        `;

        // Show detailed guess history if available
        if (game.guesses && game.guesses.length > 0) {
            html += `
                <div style="margin-top: 20px;">
                    <h5 style="color: var(--bg-primary); margin-bottom: 10px;">Guess History:</h5>
                    <table style="width: 100%; border-collapse: collapse;">
                        <thead>
                            <tr style="background: #f1f5f9; color: var(--bg-primary); font-size: 0.85em; text-transform: uppercase;">
                                <th style="padding: 10px; text-align: left; border-bottom: 2px solid #cbd5e1;">Attempt</th>
                                <th style="padding: 10px; text-align: left; border-bottom: 2px solid #cbd5e1;">Guess</th>
                                <th style="padding: 10px; text-align: left; border-bottom: 2px solid #cbd5e1;">Response</th>
                                <th style="padding: 10px; text-align: right; border-bottom: 2px solid #cbd5e1;">Remaining Words</th>
                                <th style="padding: 10px; text-align: right; border-bottom: 2px solid #cbd5e1;">Reduction %</th>
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
                    <tr style="border-bottom: 1px solid #e2e8f0; color: var(--bg-primary);">
                        <td style="padding: 12px; font-weight: 700; color: var(--bg-primary);">#${guessData.attempt}</td>
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

// Dictionary Viewer Functions
async function toggleDictionaryViewer() {
    const viewer = document.getElementById('dictionaryViewer');
    const isVisible = viewer.style.display !== 'none';
    const wordCount = document.getElementById('dictionaryWordCount').textContent;
    
    if (isVisible) {
        viewer.style.display = 'none';
        document.getElementById('viewDictionaryBtn').innerHTML = `View Words (<span id="dictionaryWordCount">${wordCount}</span>)`;
    } else {
        await loadDictionaryWords();
        viewer.style.display = 'block';
        document.getElementById('viewDictionaryBtn').innerHTML = `Hide Words (<span id="dictionaryWordCount">${wordCount}</span>)`;
    }
}

async function loadDictionaryWords() {
    if (!currentGameId) {
        showStatus('No active game', 'error');
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE}/games/${currentGameId}/words`);
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        displayDictionaryWords(data.words);
        document.getElementById('dictionaryWordCount').textContent = data.count;
    } catch (error) {
        console.error('Failed to load dictionary words:', error);
        showStatus('Failed to load dictionary words: ' + error.message, 'error');
    }
}

function displayDictionaryWords(words) {
    const listDiv = document.getElementById('dictionaryWordList');
    listDiv.innerHTML = '';
    
    if (words.length === 0) {
        listDiv.innerHTML = '<div style=\"grid-column: 1/-1; text-align: center; color: var(--text-secondary); padding: 20px;\">No valid words remaining</div>';
        return;
    }
    
    words.forEach(word => {
        const wordSpan = document.createElement('span');
        wordSpan.textContent = word.toUpperCase();
        wordSpan.style.cssText = 'padding: 6px; background: var(--bg-secondary); border-radius: 4px; text-align: center; font-weight: 600; cursor: pointer; transition: all 0.2s;';
        wordSpan.onmouseover = function() {
            this.style.background = 'var(--accent-primary)';
            this.style.color = 'white';
        };
        wordSpan.onmouseout = function() {
            this.style.background = 'var(--bg-secondary)';
            this.style.color = '';
        };
        wordSpan.onclick = function() {
            // Copy word to clipboard
            navigator.clipboard.writeText(word.toUpperCase()).then(() => {
                showStatus(`Copied ${word.toUpperCase()} to clipboard`, 'success');
                setTimeout(hideStatus, 2000);
            });
        };
        listDiv.appendChild(wordSpan);
    });
}

// ===== AUTOPLAY FUNCTIONALITY =====

let autoplayState = {
    isRunning: false,
    shouldStop: false,
    gameCount: 0,
    gamesCompleted: 0,
    strategy: 'RANDOM',
    sessionStats: null
};

function showAutoplayModal() {
    const modal = document.getElementById('autoplayModal');
    modal.style.display = 'flex';
    
    // Set default word length to 5 and populate dictionaries
    document.getElementById('autoplayWordLength').value = '5';
    filterAutoplayDictionaries();
}

function filterAutoplayDictionaries() {
    const wordLength = parseInt(document.getElementById('autoplayWordLength').value);
    const dictionarySelect = document.getElementById('autoplayDictionary');
    
    // Clear existing options except the first "Use first available" option
    dictionarySelect.innerHTML = '<option value="">Use first available</option>';
    
    // Filter and add dictionaries matching the selected word length
    const matchingDicts = availableDictionaries.filter(d => d.available && d.wordLength === wordLength);
    
    matchingDicts.forEach(dict => {
        const option = document.createElement('option');
        option.value = dict.id;
        option.textContent = dict.name;
        if (dict.description) {
            option.title = dict.description;
        }
        dictionarySelect.appendChild(option);
    });
    
    if (matchingDicts.length === 0) {
        const option = document.createElement('option');
        option.value = '';
        option.textContent = `No ${wordLength}-letter dictionaries available`;
        option.disabled = true;
        dictionarySelect.appendChild(option);
    }
}

function hideAutoplayModal() {
    const modal = document.getElementById('autoplayModal');
    modal.style.display = 'none';
}

function toggleAutoplay() {
    if (autoplayState.isRunning) {
        // Stop autoplay
        stopAutoplay();
    } else {
        // Show modal to start autoplay
        showAutoplayModal();
    }
}

function stopAutoplay() {
    if (!autoplayState.isRunning) {
        return;
    }
    
    autoplayState.shouldStop = true;
    
    const autoplayBtn = document.getElementById('autoplayBtn');
    autoplayBtn.textContent = 'Stopping...';
    autoplayBtn.disabled = true;
    
    showStatus('Stopping autoplay after current game completes...', 'info');
}

function updateAutoplayButton(isRunning) {
    const autoplayBtn = document.getElementById('autoplayBtn');
    if (isRunning) {
        autoplayBtn.textContent = 'Stop Autoplay';
        autoplayBtn.classList.remove('btn-info');
        autoplayBtn.classList.add('btn-danger');
        autoplayBtn.disabled = false;
    } else {
        autoplayBtn.textContent = 'Autoplay Games';
        autoplayBtn.classList.remove('btn-danger');
        autoplayBtn.classList.add('btn-info');
        autoplayBtn.disabled = false;
    }
}

async function startAutoplay() {
    if (autoplayState.isRunning) {
        showStatus('Autoplay is already running!', 'error');
        return;
    }
    
    const gameCount = parseInt(document.getElementById('autoplayGameCount').value);
    const strategy = document.getElementById('autoplayStrategy').value;
    const wordLength = parseInt(document.getElementById('autoplayWordLength').value);
    const selectedDict = document.getElementById('autoplayDictionary').value;
    const guessDelay = parseInt(document.getElementById('autoplayDelay').value) || 1000;
    
    if (gameCount < 1 || gameCount > 1000) {
        showStatus('Please enter a number between 1 and 1000', 'error');
        return;
    }
    
    // Validate dictionary selection for chosen word length
    if (!selectedDict) {
        const availableDict = availableDictionaries.find(d => d.available && d.wordLength === wordLength);
        if (!availableDict) {
            showStatus(`No ${wordLength}-letter dictionaries available`, 'error');
            return;
        }
    }
    
    hideAutoplayModal();
    
    // Create new session
    try {
        sessionStorage.removeItem('gameHistory');
        sessionStorage.removeItem('gameStats');
        updateHistoryDisplay();
        updateStats();
        helpUsedCount = 0;
        updateHelpCounter();
        
        autoplayState.isRunning = true;
        autoplayState.shouldStop = false;
        autoplayState.gameCount = gameCount;
        autoplayState.gamesCompleted = 0;
        autoplayState.strategy = strategy;
        
        document.getElementById('dictionarySelector').disabled = true;
        document.getElementById('guessBtn').disabled = true;
        
        updateAutoplayButton(true);
        
        showStatus(`Starting autoplay: ${gameCount} ${wordLength}-letter games with ${strategy} strategy (${guessDelay}ms delay)`, 'success');
        
        await runAutoplayGames(selectedDict, strategy, wordLength, guessDelay);
        
        autoplayState.isRunning = false;
        autoplayState.shouldStop = false;
        document.getElementById('dictionarySelector').disabled = false;
        
        updateAutoplayButton(false);
        
        const statusMessage = autoplayState.gamesCompleted < gameCount 
            ? `Autoplay stopped after ${autoplayState.gamesCompleted} games. Displaying session statistics...`
            : 'Autoplay completed! Displaying session statistics...';
        showStatus(statusMessage, 'success');
        showSessionViewer();
        
    } catch (error) {
        autoplayState.isRunning = false;
        autoplayState.shouldStop = false;
        document.getElementById('dictionarySelector').disabled = false;
        updateAutoplayButton(false);
        showStatus('Autoplay failed: ' + error.message, 'error');
    }
}

async function runAutoplayGames(dictionaryId, strategy, wordLength, guessDelay = 1000) {
    for (let i = 0; i < autoplayState.gameCount; i++) {
        // Check if stop was requested
        if (autoplayState.shouldStop) {
            console.log('Autoplay stop requested, finishing after current game');
            break;
        }
        
        autoplayState.gamesCompleted = i;
        
        try {
            // If no specific dictionary selected, find first available for word length
            let dictToUse = dictionaryId;
            if (!dictToUse) {
                const availableDict = availableDictionaries.find(d => d.available && d.wordLength === wordLength);
                if (availableDict) {
                    dictToUse = availableDict.id;
                }
            }
            
            const requestBody = dictToUse ? { dictionaryId: dictToUse } : {};
            
            const createResponse = await fetch(`${API_BASE}/games`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(requestBody)
            });
            
            if (!createResponse.ok) {
                throw new Error('Failed to create game');
            }
            
            const gameData = await createResponse.json();
            const gameId = gameData.gameId;
            
            if (!gameId) {
                console.error('No gameId returned from API:', gameData);
                throw new Error('Failed to create game - no gameId returned');
            }
            
            console.log(`Created game ${i + 1}/${autoplayState.gameCount} with ID: ${gameId}`);
            
            currentGameId = gameId;
            currentWordLength = gameData.wordLength || 5;
            currentDictionarySize = gameData.dictionarySize || 2315;
            currentGameGuesses = [];
            gameEnded = false;
            helpUsedCount = 0;
            
            // Update UI elements
            document.getElementById('attempts').textContent = '0';
            document.getElementById('maxAttempts').textContent = gameData.maxAttempts || 6;
            
            // Adjust letter inputs for the word length
            adjustLetterInputGrid(currentWordLength);
            
            await fetch(`${API_BASE}/games/${gameId}/strategy`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ strategy: strategy })
            });
            
            document.getElementById('guessHistory').innerHTML = '';
            
            // Initialize analytics with the game data
            if (gameData.dictionaryMetrics) {
                initializeAnalytics(gameData.dictionaryMetrics);
            } else {
                resetAnalytics();
            }
            
            const strategyName = getStrategyDisplayName(strategy);
            showStatus(`${strategyName}, Game ${i + 1}/${autoplayState.gameCount}`, 'info');
            
            await playAutoplayGame(gameId, strategy, guessDelay);
            
            try {
                await fetch(`${API_BASE}/games/${gameId}`, {
                    method: 'DELETE'
                });
            } catch (e) {
                console.warn('Failed to delete game session:', e);
            }
            
            // Use configurable delay between games (if delay > 0)
            if (guessDelay > 0) {
                await new Promise(resolve => setTimeout(resolve, guessDelay));
            }
            
        } catch (error) {
            console.error(`Error in game ${i + 1}:`, error);
            showStatus(`Error in game ${i + 1}: ${error.message} - Continuing...`, 'warning');
            // Continue to next game despite error with shorter delay
            if (guessDelay > 0) {
                await new Promise(resolve => setTimeout(resolve, Math.min(500, guessDelay)));
            }
        }
    }
}

async function playAutoplayGame(gameId, strategy, guessDelay = 1000) {
    let attemptCount = 0;
    const maxAttempts = 6;
    
    while (attemptCount < maxAttempts && !gameEnded) {
        try {
            // Fetch suggestion
            const suggestionResponse = await fetch(`${API_BASE}/games/${gameId}/suggestion`);
            
            if (!suggestionResponse.ok) {
                console.error(`Suggestion API failed with status ${suggestionResponse.status}`);
                // If suggestion fails, try to get game state and end gracefully
                try {
                    const statusResponse = await fetch(`${API_BASE}/games/${gameId}`);
                    if (statusResponse.ok) {
                        const statusData = await statusResponse.json();
                        const targetWord = statusData.targetWord || '?????';
                        saveGameResult(targetWord, attemptCount || 1, false);
                        return;
                    }
                } catch (e) {
                    console.error('Failed to fetch game state after suggestion error:', e);
                }
                throw new Error('Failed to get suggestion');
            }
            
            const suggestionData = await suggestionResponse.json();
            
            if (!suggestionData.suggestion) {
                console.error('No suggestion returned from API');
                // No valid words left - end game gracefully
                try {
                    const statusResponse = await fetch(`${API_BASE}/games/${gameId}`);
                    if (statusResponse.ok) {
                        const statusData = await statusResponse.json();
                        const targetWord = statusData.targetWord || '?????';
                        saveGameResult(targetWord, attemptCount || 1, false);
                        return;
                    }
                } catch (e) {
                    console.error('Failed to fetch game state after no suggestion:', e);
                }
                throw new Error('No valid words available');
            }
            
            const guess = suggestionData.suggestion.toUpperCase();
            console.log(`Autoplay attempt ${attemptCount + 1}: ${guess}`);
            
            // Make guess
            const guessResponse = await fetch(`${API_BASE}/games/${gameId}/guess`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ word: guess })
            });
            
            if (!guessResponse.ok) {
                const errorData = await guessResponse.json().catch(() => ({}));
                console.error('Guess API failed:', errorData);
                throw new Error(errorData.message || 'Failed to make guess');
            }
            
            const guessData = await guessResponse.json();
            attemptCount = guessData.attemptNumber;
            
            // Update UI
            document.getElementById('attempts').textContent = attemptCount;
            
            addGuessToHistory(guess, guessData.results);
            
            if (guessData.remainingWordsCount !== undefined) {
                updateAnalytics(guess, guessData.remainingWordsCount, guessData.dictionaryMetrics);
            }
            
            currentGameGuesses.push({
                attempt: attemptCount,
                guess: guess,
                results: guessData.results,
                remainingWords: guessData.remainingWordsCount,
                dictionaryMetrics: guessData.dictionaryMetrics
            });
            
            // Check game status
            if (guessData.gameWon) {
                console.log(`Game won with word: ${guess}`);
                saveGameResult(guess, attemptCount, true);
                return;
            }
            
            if (guessData.gameOver) {
                console.log('Game over, fetching target word...');
                try {
                    const statusResponse = await fetch(`${API_BASE}/games/${gameId}`);
                    if (statusResponse.ok) {
                        const statusData = await statusResponse.json();
                        const targetWord = statusData.targetWord || '?????';
                        console.log(`Game lost, target word was: ${targetWord}`);
                        saveGameResult(targetWord, attemptCount, false);
                    } else {
                        console.warn('Failed to fetch game status');
                        saveGameResult('?????', attemptCount, false);
                    }
                } catch (e) {
                    console.error('Error fetching target word:', e);
                    saveGameResult('?????', attemptCount, false);
                }
                return;
            }
            
            // Wait configured delay before next guess (if delay > 0)
            if (guessDelay > 0) {
                await new Promise(resolve => setTimeout(resolve, guessDelay));
            }
            
        } catch (error) {
            console.error('Error during autoplay guess:', error);
            throw error;
        }
    }
    
    // Max attempts reached
    console.log('Max attempts reached, fetching target word...');
    try {
        const statusResponse = await fetch(`${API_BASE}/games/${gameId}`);
        if (statusResponse.ok) {
            const statusData = await statusResponse.json();
            const targetWord = statusData.targetWord || '?????';
            console.log(`Max attempts, target word was: ${targetWord}`);
            saveGameResult(targetWord, maxAttempts, false);
        } else {
            console.warn('Failed to fetch game status');
            saveGameResult('?????', maxAttempts, false);
        }
    } catch (e) {
        console.error('Error fetching target word:', e);
        saveGameResult('?????', maxAttempts, false);
    }
}

// Initialize dictionaries on page load
loadDictionaries();