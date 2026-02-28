/**
 * Autoplay engine — runs the bot for N games using the API.
 * Imports: state, ui, analytics, api.
 */
import { state } from './state.js';
import { showStatus } from './ui.js';
import { initializeAnalytics, resetAnalytics, updateAnalytics } from './analytics.js';
import {
    apiCreateGame, apiDeleteGame, apiMakeGuess, apiGetSuggestion,
    apiSetStrategy, apiGetGameState,
} from './api.js';

// ---- Setup helpers ----

export function showAutoplayModal() {
    const wordLengthEl = document.getElementById('autoplayWordLength');
    if (wordLengthEl && !wordLengthEl.value) {
        const defaultDict = state.availableDictionaries.find(d => d.available && d.wordLength === 5)
            || state.availableDictionaries.find(d => d.available);
        if (defaultDict) wordLengthEl.value = String(defaultDict.wordLength);
    }
    filterAutoplayDictionaries();
}

export function filterAutoplayDictionaries() {
    const wordLength      = parseInt(document.getElementById('autoplayWordLength').value);
    const dictionarySelect = document.getElementById('autoplayDictionary');
    dictionarySelect.innerHTML = '<option value="">Use first available</option>';

    const matchingDicts = state.availableDictionaries.filter(d => d.available && d.wordLength === wordLength);
    matchingDicts.forEach(dict => {
        const option = document.createElement('option');
        option.value = dict.id;
        option.textContent = dict.name;
        if (dict.description) option.title = dict.description;
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

export function toggleAutoplay() {
    if (state.autoplayState.isRunning) {
        stopAutoplay();
    } else {
        showAutoplayModal();
        startAutoplay();
    }
}

export function stopAutoplay() {
    if (!state.autoplayState.isRunning) return;
    state.autoplayState.shouldStop = true;
    const autoplayBtn = document.getElementById('autoplayBtn');
    if (autoplayBtn) { autoplayBtn.textContent = 'Stopping...'; autoplayBtn.disabled = true; }
    showStatus('Stopping autoplay after current game completes\u2026', 'info');
}

function _updateAutoplayButton(isRunning) {
    const autoplayBtn = document.getElementById('autoplayBtn');
    if (!autoplayBtn) return;
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

function _updateAutoplayProgress(text, percent) {
    const progressText = document.getElementById('autoplayProgressText');
    const progressBar  = document.getElementById('autoplayProgressBar');
    if (progressText) progressText.textContent = text;
    if (progressBar)  progressBar.value = Math.max(0, Math.min(100, percent));
}

function _setBotDemoRunningUi(isRunning) {
    const botDemoScreen = document.getElementById('screen-bot-demo');
    if (botDemoScreen) botDemoScreen.classList.toggle('bot-demo-running', !!isRunning);
    const setupPanel = document.getElementById('autoplaySetupPanel');
    if (setupPanel && setupPanel.tagName === 'DETAILS' && isRunning) setupPanel.open = false;
}

// ---- Core autoplay ----

export async function startAutoplay() {
    const as = state.autoplayState;
    if (as.isRunning) { showStatus('Autoplay is already running!', 'error'); return; }

    const gameCount  = parseInt(document.getElementById('autoplayGameCount').value);
    const strategy   = document.getElementById('autoplayStrategy').value;
    const wordLength = parseInt(document.getElementById('autoplayWordLength').value);
    const selectedDict = document.getElementById('autoplayDictionary').value;
    const guessDelay = parseInt(document.getElementById('autoplayDelay').value) || 1000;

    if (gameCount < 1 || gameCount > 1000) {
        showStatus('Please enter a number between 1 and 1000', 'error');
        return;
    }

    if (!selectedDict) {
        const availableDict = state.availableDictionaries.find(d => d.available && d.wordLength === wordLength);
        if (!availableDict) {
            showStatus(`No ${wordLength}-letter dictionaries available`, 'error');
            return;
        }
    }

    try {
        sessionStorage.removeItem('gameHistory');
        sessionStorage.removeItem('gameStats');

        // Notify game.js to refresh history/stats displays
        document.dispatchEvent(new CustomEvent('wordai:refreshHistory'));

        state.helpUsedCount = 0;
        document.dispatchEvent(new CustomEvent('wordai:updateHelpCounter'));

        as.isRunning      = true;
        as.shouldStop     = false;
        as.gameCount      = gameCount;
        as.gamesCompleted = 0;
        as.strategy       = strategy;

        const globalDictSelector = document.getElementById('dictionarySelector');
        if (globalDictSelector) globalDictSelector.disabled = true;
        const guessBtn = document.getElementById('guessBtn');
        if (guessBtn) guessBtn.disabled = true;

        _updateAutoplayButton(true);
        _setBotDemoRunningUi(true);
        _updateAutoplayProgress(`Starting: 0/${gameCount} games`, 0);
        showStatus(`Starting autoplay: ${gameCount} ${wordLength}-letter games with ${strategy} strategy (${guessDelay}ms delay)`, 'success');

        await _runAutoplayGames(selectedDict, strategy, wordLength, guessDelay);

        as.isRunning  = false;
        as.shouldStop = false;
        _setBotDemoRunningUi(false);
        if (globalDictSelector) globalDictSelector.disabled = false;
        if (guessBtn) guessBtn.disabled = false;
        _updateAutoplayButton(false);

        const completed = as.gamesCompleted;
        const pct = gameCount > 0 ? Math.round((completed / gameCount) * 100) : 0;
        _updateAutoplayProgress(`Completed: ${completed}/${gameCount} games`, pct);

        const statusMessage = completed < gameCount
            ? `Autoplay stopped after ${completed} games. Displaying session statistics\u2026`
            : 'Autoplay completed! Displaying session statistics\u2026';
        showStatus(statusMessage, 'success');
        document.dispatchEvent(new CustomEvent('wordai:showSessionViewer'));

    } catch (error) {
        const as2 = state.autoplayState;
        as2.isRunning  = false;
        as2.shouldStop = false;
        _setBotDemoRunningUi(false);
        const gds = document.getElementById('dictionarySelector');
        if (gds) gds.disabled = false;
        const gb = document.getElementById('guessBtn');
        if (gb) gb.disabled = false;
        _updateAutoplayButton(false);
        _updateAutoplayProgress('Failed', 0);
        showStatus('Autoplay failed: ' + error.message, 'error');
    }
}

async function _runAutoplayGames(dictionaryId, strategy, wordLength, guessDelay) {
    const as = state.autoplayState;

    for (let i = 0; i < as.gameCount; i++) {
        if (as.shouldStop) break;

        as.gamesCompleted = i;
        const pct = as.gameCount > 0 ? Math.round((i / as.gameCount) * 100) : 0;
        _updateAutoplayProgress(`Running: ${i + 1}/${as.gameCount} games`, pct);

        try {
            let dictToUse = dictionaryId;
            if (!dictToUse) {
                const availableDict = state.availableDictionaries.find(d => d.available && d.wordLength === wordLength);
                if (availableDict) dictToUse = availableDict.id;
            }

            const requestBody = dictToUse ? { dictionaryId: dictToUse } : {};
            const createResponse = await apiCreateGame(requestBody);
            if (!createResponse.ok) throw new Error('Failed to create game');

            const gameData = await createResponse.json();
            const gameId   = gameData.gameId;
            if (!gameId) throw new Error('Failed to create game - no gameId returned');

            // Update shared state
            state.currentGameId       = gameId;
            state.currentWordLength   = gameData.wordLength || 5;
            state.currentDictionarySize = gameData.dictionarySize || 2315;
            state.currentGameGuesses  = [];
            state.gameEnded           = false;
            state.helpUsedCount       = 0;
            state.letterStatusMap     = {};
            state.latestOccurrenceData = null;

            document.getElementById('attempts').textContent   = '0';
            document.getElementById('maxAttempts').textContent = gameData.maxAttempts || 6;
            document.dispatchEvent(new CustomEvent('wordai:adjustLetterInputGrid', { detail: state.currentWordLength }));

            await apiSetStrategy(gameId, strategy);
            document.getElementById('guessHistory').innerHTML = '';

            if (gameData.dictionaryMetrics) {
                initializeAnalytics(gameData.dictionaryMetrics);
            } else {
                resetAnalytics();
            }

            await _playAutoplayGame(gameId, strategy, guessDelay);
            as.gamesCompleted = i + 1;
            const pctDone = as.gameCount > 0 ? Math.round(((i + 1) / as.gameCount) * 100) : 0;
            _updateAutoplayProgress(`Completed: ${i + 1}/${as.gameCount} games`, pctDone);

            try { await apiDeleteGame(gameId); } catch (e) { console.warn('Failed to delete game session:', e); }

            if (guessDelay > 0) await new Promise(resolve => setTimeout(resolve, guessDelay));

        } catch (error) {
            console.error(`Error in game ${i + 1}:`, error);
            showStatus(`Error in game ${i + 1}: ${error.message} - Continuing\u2026`, 'warning');
            if (guessDelay > 0) await new Promise(resolve => setTimeout(resolve, Math.min(500, guessDelay)));
        }
    }
}

async function _playAutoplayGame(gameId, strategy, guessDelay) {
    let attemptCount = 0;
    const maxAttempts = 6;

    while (attemptCount < maxAttempts && !state.gameEnded) {
        try {
            const suggestionResponse = await apiGetSuggestion(gameId);

            if (!suggestionResponse.ok) {
                try {
                    const statusResponse = await apiGetGameState(gameId);
                    if (statusResponse.ok) {
                        const statusData = await statusResponse.json();
                        document.dispatchEvent(new CustomEvent('wordai:saveGameResult', {
                            detail: { targetWord: statusData.targetWord || '?????', attempts: attemptCount || 1, won: false },
                        }));
                        return;
                    }
                } catch (e) { /* ignore */ }
                throw new Error('Failed to get suggestion');
            }

            const suggestionData = await suggestionResponse.json();

            if (!suggestionData.suggestion) {
                try {
                    const statusResponse = await apiGetGameState(gameId);
                    if (statusResponse.ok) {
                        const statusData = await statusResponse.json();
                        document.dispatchEvent(new CustomEvent('wordai:saveGameResult', {
                            detail: { targetWord: statusData.targetWord || '?????', attempts: attemptCount || 1, won: false },
                        }));
                        return;
                    }
                } catch (e) { /* ignore */ }
                throw new Error('No valid words available');
            }

            const guess = suggestionData.suggestion.toUpperCase();
            const guessResponse = await apiMakeGuess(gameId, guess);

            if (!guessResponse.ok) {
                const errorData = await guessResponse.json().catch(() => ({}));
                throw new Error(errorData.message || 'Failed to make guess');
            }

            const guessData = await guessResponse.json();
            attemptCount = guessData.attemptNumber;

            document.getElementById('attempts').textContent = attemptCount;

            document.dispatchEvent(new CustomEvent('wordai:addGuessToHistory', {
                detail: { word: guess, results: guessData.results },
            }));

            if (guessData.remainingWordsCount !== undefined) {
                updateAnalytics(guess, guessData.remainingWordsCount, guessData.dictionaryMetrics);
            }

            state.currentGameGuesses.push({
                attempt:          attemptCount,
                guess:            guess,
                results:          guessData.results,
                remainingWords:   guessData.remainingWordsCount,
                dictionaryMetrics: guessData.dictionaryMetrics,
            });

            if (guessData.gameWon) {
                document.dispatchEvent(new CustomEvent('wordai:saveGameResult', {
                    detail: { targetWord: guess, attempts: attemptCount, won: true },
                }));
                return;
            }

            if (guessData.gameOver) {
                try {
                    const statusResponse = await apiGetGameState(gameId);
                    const statusData     = statusResponse.ok ? await statusResponse.json() : {};
                    document.dispatchEvent(new CustomEvent('wordai:saveGameResult', {
                        detail: { targetWord: statusData.targetWord || '?????', attempts: attemptCount, won: false },
                    }));
                } catch (e) {
                    document.dispatchEvent(new CustomEvent('wordai:saveGameResult', {
                        detail: { targetWord: '?????', attempts: attemptCount, won: false },
                    }));
                }
                return;
            }

            if (guessDelay > 0) await new Promise(resolve => setTimeout(resolve, guessDelay));

        } catch (error) {
            console.error('Error during autoplay guess:', error);
            throw error;
        }
    }

    // Max attempts reached
    try {
        const statusResponse = await apiGetGameState(gameId);
        const statusData     = statusResponse.ok ? await statusResponse.json() : {};
        document.dispatchEvent(new CustomEvent('wordai:saveGameResult', {
            detail: { targetWord: statusData.targetWord || '?????', attempts: maxAttempts, won: false },
        }));
    } catch (e) {
        document.dispatchEvent(new CustomEvent('wordai:saveGameResult', {
            detail: { targetWord: '?????', attempts: maxAttempts, won: false },
        }));
    }
}
