/**
 * Bot Performance screen — runs the selected algorithm against every word in a
 * dictionary and displays summary / detail tables.
 * Imports: state, ui, api.
 */
import { state } from './state.js';
import { showStatus } from './ui.js';
import {
    apiGetDictionary, apiCreateGame, apiDeleteGame,
    apiMakeGuess, apiGetSuggestion, apiSetStrategy,
} from './api.js';
import { getBrowserSessionId } from './browser-session.js';

// ---- UI setup helpers ----

export function showAnalysisModal() {
    const dictionarySelect = document.getElementById('analysisDictionary');
    if (!dictionarySelect) return;
    dictionarySelect.innerHTML = '';
    state.availableDictionaries.forEach(dict => {
        if (dict.available) {
            const option = document.createElement('option');
            option.value = dict.id;
            option.textContent = dict.name;
            if (dict.wordLength === 5) option.selected = true;
            dictionarySelect.appendChild(option);
        }
    });
}

export function cancelPlayerAnalysis() {
    if (!state.analysisState.running) return;
    state.analysisState.shouldStop = true;
    const cancelBtn = document.getElementById('cancelAnalysisBtn');
    if (cancelBtn) { cancelBtn.textContent = 'Cancelling\u2026'; cancelBtn.disabled = true; }
    showStatus('Cancelling analysis after the current word completes\u2026', 'info');
}

function _updateAnalysisProgress(text, percent) {
    const progressText = document.getElementById('analysisProgressText');
    const progressBar  = document.getElementById('analysisProgressBar');
    if (progressText) progressText.textContent = text;
    if (progressBar)  progressBar.value = Math.max(0, Math.min(100, percent));
}

function _setBotPerformanceRunningUi(isRunning) {
    const performanceScreen = document.getElementById('screen-bot-performance');
    if (performanceScreen) performanceScreen.classList.toggle('bot-performance-running', !!isRunning);
    const setupPanel = document.getElementById('analysisSetupPanel');
    if (setupPanel && setupPanel.tagName === 'DETAILS' && isRunning) setupPanel.open = false;
}

// ---- Main entry ----

export async function startPlayerAnalysis() {
    const strategy   = document.getElementById('analysisStrategy').value;
    const dictionaryId = document.getElementById('analysisDictionary').value;
    const guessDelay = parseInt(document.getElementById('analysisDelay').value) || 0;

    const startBtn  = document.getElementById('startAnalysisBtn');
    const cancelBtn = document.getElementById('cancelAnalysisBtn');
    if (startBtn)  startBtn.disabled  = true;
    if (cancelBtn) { cancelBtn.disabled = false; cancelBtn.textContent = 'Cancel'; }

    try {
        _setBotPerformanceRunningUi(true);

        const dictResponse = await apiGetDictionary(dictionaryId);
        if (!dictResponse.ok) throw new Error('Failed to load dictionary');
        const dictData = await dictResponse.json();
        const allWords = dictData.words || [];

        const as = state.analysisState;
        as.running     = true;
        as.shouldStop  = false;
        as.completed   = 0;
        as.total       = allWords.length;
        as.summaryData = [];
        as.detailsData = [];

        _updateAnalysisProgress(`Starting: 0/${allWords.length}`, 0);
        showStatus(`Starting Player Analysis: Testing ${allWords.length} words with ${strategy}`, 'info');

        await _runAnalysisGames(allWords, dictionaryId, strategy, dictData.wordLength, guessDelay);
        _displayFinalAnalysisResults();

    } catch (error) {
        console.error('Player Analysis error:', error);
        showStatus('Analysis failed: ' + error.message, 'error');
        _updateAnalysisProgress('Failed', 0);
    } finally {
        state.analysisState.running = false;
        _setBotPerformanceRunningUi(false);
        if (startBtn)  startBtn.disabled  = false;
        if (cancelBtn) { cancelBtn.disabled = true; cancelBtn.textContent = 'Cancel'; }
    }
}

async function _runAnalysisGames(targetWords, dictionaryId, strategy, wordLength, guessDelay) {
    const as = state.analysisState;

    for (let i = 0; i < targetWords.length; i++) {
        if (as.shouldStop) {
            showStatus(`Analysis cancelled at ${as.completed}/${as.total}`, 'warning');
            break;
        }

        const targetWord = targetWords[i];
        as.completed = i + 1;

        const pct = as.total > 0 ? Math.round((as.completed / as.total) * 100) : 0;
        _updateAnalysisProgress(`Running: ${as.completed}/${as.total} ("${targetWord.toUpperCase()}")`, pct);

        try {
            const createResponse = await apiCreateGame({
                dictionaryId,
                targetWord,
                browserSessionId: getBrowserSessionId(),
            });
            if (!createResponse.ok) throw new Error('Failed to create game');
            const gameData = await createResponse.json();
            const gameId   = gameData.gameId;

            await apiSetStrategy(gameId, strategy);
            showStatus(`Analysis: ${i + 1}/${targetWords.length} - Testing "${targetWord.toUpperCase()}" with ${strategy}`, 'info');

            const gameResult = await _playAnalysisGame(gameId, targetWord, strategy, gameData.maxAttempts || 6);
            as.summaryData.push(gameResult.summary);
            as.detailsData.push(...gameResult.details);

            try { await apiDeleteGame(gameId); } catch (e) { /* ignore */ }
            if (guessDelay > 0) await new Promise(resolve => setTimeout(resolve, guessDelay));

        } catch (error) {
            console.error(`Error analyzing word "${targetWord}":`, error);
        }
    }
}

async function _playAnalysisGame(gameId, targetWord, strategy, maxAttempts = 6) {
    let attempt = 0;
    let won = false;
    let localGameEnded = false;
    const details = [];
    const guesses  = [];

    while (!localGameEnded && attempt < maxAttempts) {
        try {
            const suggestionResponse = await apiGetSuggestion(gameId);
            if (!suggestionResponse.ok) { console.error('Failed to get suggestion'); break; }
            const suggestionData = await suggestionResponse.json();
            const suggestedWord  = suggestionData.suggestion;
            if (!suggestedWord) { console.error('No suggestion returned'); break; }

            const guessResponse = await apiMakeGuess(gameId, suggestedWord.toLowerCase());
            if (!guessResponse.ok) {
                const errorData = await guessResponse.json();
                console.error('Guess failed:', errorData.message);
                break;
            }

            const guessData = await guessResponse.json();
            attempt = guessData.attemptNumber;
            guesses.push(suggestedWord.toUpperCase());

            details.push({
                iteration:      state.analysisState.completed,
                attempt,
                targetWord:     targetWord.toUpperCase(),
                guess:          suggestedWord.toUpperCase(),
                remainingWords: guessData.remainingWordsCount || 0,
                letterCount:    suggestedWord.length,
                winner:         guessData.gameWon ? 'true' : 'false',
            });

            if (guessData.gameWon)  { won = true; localGameEnded = true; break; }
            if (guessData.gameOver) { localGameEnded = true; break; }

        } catch (error) {
            console.error('Error during guess:', error);
            break;
        }
    }

    return {
        summary: {
            iteration:  state.analysisState.completed,
            targetWord: targetWord.toUpperCase(),
            algorithm:  strategy,
            attempts:   attempt,
            guesses:    guesses.join(','),
            result:     won ? 'WON' : 'LOST',
        },
        details,
    };
}

// ---- Results display ----

function _displayFinalAnalysisResults() {
    const as = state.analysisState;
    const totalGames = as.summaryData.length;
    const gamesWon   = as.summaryData.filter(g => g.result === 'WON').length;
    const gamesLost  = totalGames - gamesWon;
    const winRate    = totalGames > 0 ? (gamesWon * 100.0 / totalGames) : 0;

    const wonAttempts  = as.summaryData.filter(g => g.result === 'WON').map(g => g.attempts);
    const minAttempts  = wonAttempts.length > 0 ? Math.min(...wonAttempts) : null;
    const maxAttempts  = wonAttempts.length > 0 ? Math.max(...wonAttempts) : null;
    const avgAttempts  = wonAttempts.length > 0 ? wonAttempts.reduce((a, b) => a + b, 0) / wonAttempts.length : null;
    const winRateDisplay = winRate === 100 ? '100' : winRate.toFixed(2);
    const avgDisplay     = avgAttempts ? avgAttempts.toFixed(2) : 'N/A';

    document.getElementById('analysisResults').style.display = 'block';

    document.getElementById('analysisSummary').innerHTML = `
        <table class="analysis-table">
            <thead><tr><th>Metric</th><th class="num">Value</th></tr></thead>
            <tbody>
                <tr><td>Total Games</td><td class="num">${totalGames}</td></tr>
                <tr><td>Games Won</td><td class="num">${gamesWon}</td></tr>
                <tr><td>Games Lost</td><td class="num">${gamesLost}</td></tr>
                <tr><td>Win Rate</td><td class="num">${winRateDisplay}%</td></tr>
                <tr><td>Min Attempts (Won)</td><td class="num">${minAttempts || 'N/A'}</td></tr>
                <tr><td>Avg Attempts (Won)</td><td class="num">${avgDisplay}</td></tr>
                <tr><td>Max Attempts (Won)</td><td class="num">${maxAttempts || 'N/A'}</td></tr>
            </tbody>
        </table>`;

    let detailsHTML = `
        <table class="analysis-table">
            <thead><tr><th>Iteration</th><th>Target</th><th class="num">Attempts</th><th>Result</th></tr></thead>
            <tbody>`;

    as.summaryData.forEach(game => {
        const resultText  = game.result === 'WON' ? '\u2713 WON' : '\u2717 LOST';
        const resultClass = game.result === 'WON' ? 'won' : 'lost';
        detailsHTML += `
            <tr>
                <td>${game.iteration}</td>
                <td class="mono">${game.targetWord.toUpperCase()}</td>
                <td class="num">${game.attempts}</td>
                <td><span class="analysis-pill ${resultClass}">${resultText}</span></td>
            </tr>`;
    });

    detailsHTML += '</tbody></table>';
    document.getElementById('analysisDetails').innerHTML = detailsHTML;

    showStatus(`Analysis complete! ${totalGames} games tested with ${winRateDisplay}% win rate`, 'success');
    _updateAnalysisProgress(`Completed: ${totalGames}/${as.total}`, 100);
    document.getElementById('analysisResults').scrollIntoView({ behavior: 'smooth' });
}

export function hideAnalysisResults() {
    document.getElementById('analysisResults').style.display = 'none';
}

// ---- CSV downloads ----

export function downloadAnalysisSummary() {
    const as = state.analysisState;
    if (as.summaryData.length === 0) { showStatus('No analysis data to download', 'error'); return; }
    let csv = 'Iteration,TargetWord,Algorithm,Attempts,Guesses,Result\n';
    as.summaryData.forEach(game => {
        csv += `${game.iteration},${game.targetWord},${game.algorithm},${game.attempts},"${game.guesses}",${game.result}\n`;
    });
    _downloadCSV(csv, 'player-analysis-summary.csv');
}

export function downloadAnalysisDetails() {
    const as = state.analysisState;
    if (as.detailsData.length === 0) { showStatus('No analysis details to download', 'error'); return; }
    let csv = 'Iteration,Attempt,TargetWord,Guess,RemainingWords,LetterCount,Winner\n';
    as.detailsData.forEach(detail => {
        csv += `${detail.iteration},${detail.attempt},${detail.targetWord},${detail.guess},${detail.remainingWords},${detail.letterCount},${detail.winner}\n`;
    });
    _downloadCSV(csv, 'player-analysis-details.csv');
}

function _downloadCSV(content, filename) {
    const blob = new Blob([content], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url  = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', filename);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}
