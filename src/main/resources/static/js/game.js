/**
 * game.js — Entry point. Wires all ES modules together.
 * Contains: core game logic, auth, session history, session viewer,
 * and dictionary management. Everything else lives in its own module.
 */
import { state } from './state.js';
import {
    apiCreateGame, apiDeleteGame, apiMakeGuess, apiGetSuggestion,
    apiSetStrategy, apiGetGameState, apiCheckAuth, apiGetAlgorithms,
    apiGetDictionaries, apiCheckHealth,
} from './api.js';
import {
    shouldSuppressNativeKeyboard,
    getCurrentGuess, clearLetterInputs, disableLetterInputs, setupLetterInputs,
    showStatus, hideStatus, updateHelpCounter,
} from './ui.js';
import { initializeKeyboard, trackLetterStatus } from './keyboard.js';
import {
    initializeAnalytics, resetAnalytics, updateAnalytics,
    updateOccurrenceTable,
    refreshDictionaryScreen,
    sortLetterFrequency, sortDictionaryWords, filterDictionaryWords,
    switchDictTab,
} from './analytics.js';
import {
    initRouter, initMobileNavigation, switchMobileView, switchMobilePanel,
} from './navigation.js';
import { filterAutoplayDictionaries, toggleAutoplay } from './autoplay.js';
import {
    startPlayerAnalysis, cancelPlayerAnalysis,
    hideAnalysisResults, downloadAnalysisSummary, downloadAnalysisDetails,
} from './player-analysis.js';
import {
    loadAdminScreen, refreshAdminUsers,
    openRoleModal, closeRoleModal, addSelectedRole, removeRoleFromModal,
    openPasswordModal, closePasswordModal, submitPasswordReset,
    toggleUserEnabled,
} from './admin.js';

// ============================================================
// Initialisation
// ============================================================

window.addEventListener('DOMContentLoaded', function () {
    checkAuthentication();
    initRouter();
    initMobileNavigation();
    adjustLetterInputGrid(5);
    loadAlgorithms();
    newGame();
    updateHistoryDisplay();
    updateStats();
    updateHelpCounter();

    // ---- CustomEvent bridge from autoplay.js and other modules ----
    document.addEventListener('wordai:refreshHistory', () => {
        updateHistoryDisplay();
        updateStats();
    });
    document.addEventListener('wordai:updateHelpCounter', () => updateHelpCounter());
    document.addEventListener('wordai:adjustLetterInputGrid', (e) => adjustLetterInputGrid(e.detail));
    document.addEventListener('wordai:addGuessToHistory', (e) => addGuessToHistory(e.detail.word, e.detail.results));
    document.addEventListener('wordai:saveGameResult', (e) => saveGameResult(e.detail.targetWord, e.detail.attempts, e.detail.won));
    document.addEventListener('wordai:showSessionViewer', () => showSessionViewer());
    document.addEventListener('wordai:loadAdmin', () => loadAdminScreen());
});

// ============================================================
// Window exports — every function called from HTML onclick/onchange/oninput
// must be on window because ES modules are not in global scope.
// ============================================================

// game.js functions (defined below — hoisted function declarations)
window.newGame                 = newGame;
window.newSession              = newSession;
window.makeGuess               = makeGuess;
window.getSuggestion           = getSuggestion;
window.changeStrategy          = changeStrategy;
window.checkHealth             = checkHealth;
window.logout                  = logout;
window.showSessionViewer       = showSessionViewer;
window.hideSessionViewer       = hideSessionViewer;
window.exportSessionGamesToCSV = exportSessionGamesToCSV;
window.onDictionaryChange      = onDictionaryChange;
window.populateDictionarySelector = populateDictionarySelector;

// ui.js
window.hideStatus              = hideStatus;

// navigation.js
window.switchMobilePanel       = switchMobilePanel;

// analytics.js
window.sortLetterFrequency     = sortLetterFrequency;
window.sortDictionaryWords     = sortDictionaryWords;
window.filterDictionaryWords   = filterDictionaryWords;
window.switchDictTab           = switchDictTab;

// autoplay.js
window.toggleAutoplay          = toggleAutoplay;
window.filterAutoplayDictionaries = filterAutoplayDictionaries;

// player-analysis.js
window.startPlayerAnalysis     = startPlayerAnalysis;
window.cancelPlayerAnalysis    = cancelPlayerAnalysis;
window.hideAnalysisResults     = hideAnalysisResults;
window.downloadAnalysisSummary = downloadAnalysisSummary;
window.downloadAnalysisDetails = downloadAnalysisDetails;

// admin.js
window.refreshAdminUsers       = refreshAdminUsers;
window.openRoleModal           = openRoleModal;
window.closeRoleModal          = closeRoleModal;
window.addSelectedRole         = addSelectedRole;
window.removeRoleFromModal     = removeRoleFromModal;
window.openPasswordModal       = openPasswordModal;
window.closePasswordModal      = closePasswordModal;
window.submitPasswordReset     = submitPasswordReset;
window.toggleUserEnabled       = toggleUserEnabled;

// ============================================================
// Authentication
// ============================================================

async function checkAuthentication() {
    try {
        const response = await apiCheckAuth();
        if (response.ok) {
            state.currentUser = await response.json();
            displayUserInfo();
            updateUIForUserRole();
        } else {
            state.currentUser = null;
            showGuestOptions();
            updateUIForUserRole();
        }
    } catch (error) {
        console.log('Playing as guest');
        state.currentUser = null;
        showGuestOptions();
        updateUIForUserRole();
    }
}

async function loadAlgorithms() {
    try {
        const response = await apiGetAlgorithms();
        if (!response.ok) { console.error('Failed to load algorithms, status:', response.status); return; }
        const algorithms = await response.json();
        ['strategySelector', 'autoplayStrategy', 'analysisStrategy'].forEach(selectorId => {
            const selector = document.getElementById(selectorId);
            if (!selector) return;
            const currentValue = selector.value;
            selector.innerHTML = '';
            algorithms.forEach(algo => {
                if (algo.enabled === 'false') return;
                const option = document.createElement('option');
                option.value       = algo.id;
                option.textContent = algo.name;
                option.title       = algo.description;
                selector.appendChild(option);
            });
            if (currentValue) {
                const opt = Array.from(selector.options).find(o => o.value === currentValue && !o.disabled);
                if (opt) { selector.value = currentValue; }
                else {
                    const first = Array.from(selector.options).find(o => !o.disabled);
                    if (first) selector.value = first.value;
                }
            }
        });
    } catch (error) {
        console.error('Error loading algorithms:', error);
    }
}

function updateUIForUserRole() {
    const isAdmin   = _isUserAdmin();
    const isPremium = _isUserPremium();
    document.querySelectorAll('[data-role="premium"]').forEach(el => {
        el.style.display = (state.currentUser && (isPremium || isAdmin)) ? 'block' : 'none';
    });
    document.querySelectorAll('[data-role="admin"]').forEach(el => {
        el.style.display = (state.currentUser && isAdmin) ? 'block' : 'none';
    });
    document.querySelectorAll('[data-role="user"]').forEach(el => {
        el.style.display = state.currentUser ? 'block' : 'none';
    });
    _updateRoleIndicators();
}

function _isUserAdmin()   { return !!(state.currentUser?.roles?.includes('ROLE_ADMIN')); }
function _isUserPremium() { return !!(state.currentUser?.roles?.includes('ROLE_PREMIUM')); }

function _updateRoleIndicators() {
    const roleIndicator = document.getElementById('roleIndicator');
    if (roleIndicator) {
        const userRole = state.currentUser?.primaryRole || 'Guest';
        roleIndicator.textContent = userRole;
        roleIndicator.className   = 'role-badge role-' + userRole.toLowerCase().replace(' ', '-');
    }
    _updateNavigation();
}

function _updateNavigation() {
    let adminLink = document.getElementById('adminLink');
    if (_isUserAdmin()) {
        if (!adminLink) {
            adminLink = document.createElement('a');
            adminLink.id = 'adminLink';
            adminLink.href = '/admin.html';
            adminLink.textContent = 'Admin Panel';
            adminLink.style.cssText = 'margin-left:15px;color:#dc3545;text-decoration:none;';
            const userInfo = document.getElementById('userInfo');
            if (userInfo) userInfo.appendChild(adminLink);
        }
    } else if (adminLink) {
        adminLink.remove();
    }
}

function displayUserInfo() {
    if (!state.currentUser) return;
    const userNameEl   = document.getElementById('userName');
    const userInfoEl   = document.getElementById('userInfo');
    const signInLink   = document.getElementById('signInLink');
    const signInDrawer = document.getElementById('signInLinkDrawer');

    if (userNameEl && userInfoEl) {
        userNameEl.textContent = state.currentUser.fullName || state.currentUser.username || state.currentUser.email;
        userInfoEl.style.display = 'flex';
    }
    if (signInLink)   signInLink.style.display   = 'none';
    if (signInDrawer) signInDrawer.style.display  = 'none';

    const adminNavLink = document.querySelector('[data-nav="admin"]');
    if (adminNavLink) adminNavLink.style.display = _isUserAdmin() ? '' : 'none';
}

function showGuestOptions() {
    const userInfoEl   = document.getElementById('userInfo');
    const signInLink   = document.getElementById('signInLink');
    const signInDrawer = document.getElementById('signInLinkDrawer');

    if (userInfoEl) userInfoEl.style.display = 'none';

    const adminNavLink = document.querySelector('[data-nav="admin"]');
    if (adminNavLink) adminNavLink.style.display = 'none';

    if (signInLink)   signInLink.style.display   = 'inline-flex';
    if (signInDrawer) signInDrawer.style.display  = 'flex';
}

export function logout() { window.location.href = '/api/auth/logout'; }

// ============================================================
// Core game
// ============================================================

export async function newGame() {
    try {
        hideStatus();

        if (state.currentGameId) {
            try { await apiDeleteGame(state.currentGameId); }
            catch (e) { console.warn('Failed to delete previous session:', e); }
        }

        const selector    = document.getElementById('dictionarySelector');
        const dictionaryId = selector?.value || null;
        const requestBody  = dictionaryId ? { dictionaryId } : {};

        const response = await apiCreateGame(requestBody);
        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);

        const data = await response.json();
        state.currentGameId        = data.gameId;
        state.currentWordLength    = data.wordLength;
        state.gameEnded            = false;
        state.letterStatusMap      = {};
        state.latestOccurrenceData = null;

        adjustLetterInputGrid(data.wordLength);
        document.getElementById('attempts').textContent    = '0';
        document.getElementById('maxAttempts').textContent = data.maxAttempts;
        document.getElementById('guessHistory').innerHTML  = '';
        const guessBtn = document.getElementById('guessBtn');
        if (guessBtn) guessBtn.disabled = false;

        if (data.dictionaryMetrics) initializeAnalytics(data.dictionaryMetrics);
        else resetAnalytics();

        state.helpUsedCount      = 0;
        state.currentGameGuesses = [];
        updateHelpCounter();
        await changeStrategy();

        if (window.innerWidth <= 768) switchMobileView('game');
    } catch (error) {
        console.error('Failed to create new game:', error);
    }
}

export async function newSession() {
    sessionStorage.removeItem('gameHistory');
    sessionStorage.removeItem('gameStats');
    updateHistoryDisplay();
    updateStats();
    state.helpUsedCount = 0;
    updateHelpCounter();
    await newGame();
}

export async function makeGuess() {
    if (!state.currentGameId || state.gameEnded) return;

    const word = getCurrentGuess().trim();
    if (!word) { showStatus('Please enter a word!', 'error'); return; }

    const expectedLength = document.querySelectorAll('.letter-input').length;
    if (word.length !== expectedLength) {
        showStatus(`Word must be ${expectedLength} letters long!`, 'error');
        return;
    }

    try {
        const response = await apiMakeGuess(state.currentGameId, word);
        const data     = await response.json();
        if (!response.ok) throw new Error(data.message || `HTTP error! status: ${response.status}`);

        document.getElementById('attempts').textContent = data.attemptNumber;

        if (data.remainingWordsCount !== undefined) {
            updateAnalytics(word, data.remainingWordsCount, data.dictionaryMetrics);
        }

        state.currentGameGuesses.push({
            attempt:           data.attemptNumber,
            guess:             word,
            results:           data.results,
            remainingWords:    data.remainingWordsCount,
            dictionaryMetrics: data.dictionaryMetrics,
        });

        addGuessToHistory(word, data.results);
        clearLetterInputs();

        if (data.gameWon) {
            state.gameEnded = true;
            disableLetterInputs();
            const gb = document.getElementById('guessBtn');
            if (gb) gb.disabled = true;
            showStatus('\uD83C\uDF89 Congratulations! You won!', 'success');
            saveGameResult(word, data.attemptNumber, true);
        } else if (data.gameOver) {
            state.gameEnded = true;
            disableLetterInputs();
            const gb = document.getElementById('guessBtn');
            if (gb) gb.disabled = true;
            _fetchTargetWordAndSave(data.attemptNumber);
        }
    } catch (error) {
        console.error('Error making guess:', error);
        showStatus(error.message || 'Guess failed \u2014 please try again.', 'error');
    }
}

function addGuessToHistory(word, results) {
    const historyDiv = document.getElementById('guessHistory');
    const guessRow   = document.createElement('div');
    guessRow.className = 'guess-row';

    for (const r of results) {
        const box = document.createElement('div');
        box.className   = 'letter-box';
        box.textContent = r.letter;
        if      (r.status === 'G')                     box.classList.add('letter-correct');
        else if (r.status === 'A')                     box.classList.add('letter-present');
        else if (r.status === 'R' || r.status === 'X') box.classList.add('letter-absent');
        guessRow.appendChild(box);
    }

    historyDiv.appendChild(guessRow);
    historyDiv.scrollTop = historyDiv.scrollHeight;

    trackLetterStatus(results);

    if (state.latestOccurrenceData) {
        updateOccurrenceTable(state.latestOccurrenceData);
    }
}

/** Create the letter-input grid then attach all keyboard / event handlers. */
function adjustLetterInputGrid(wordLength) {
    const letterInputs = document.getElementById('letterInputs');
    if (!letterInputs) return;
    letterInputs.innerHTML = '';
    const suppressKeyboard = shouldSuppressNativeKeyboard();

    for (let i = 0; i < wordLength; i++) {
        const input = document.createElement('input');
        input.type         = 'text';
        input.className    = 'letter-input';
        input.maxLength    = 1;
        input.id           = `letter${i}`;
        input.autocomplete = 'off';
        input.spellcheck   = false;
        input.inputMode    = suppressKeyboard ? 'none' : 'text';
        input.autocapitalize = suppressKeyboard ? 'off' : 'characters';
        input.setAttribute('autocorrect', 'off');
        input.setAttribute('aria-label', `Letter ${i + 1} of ${wordLength}`);
        if (suppressKeyboard) input.setAttribute('readonly', 'readonly');
        letterInputs.appendChild(input);
    }

    setupLetterInputs(makeGuess);

    if (!suppressKeyboard && wordLength > 0) {
        document.getElementById('letter0')?.focus();
    }

    initializeKeyboard(makeGuess);
}

async function _fetchTargetWordAndSave(attempts) {
    try {
        const response   = await apiGetGameState(state.currentGameId);
        const data       = await response.json();
        const targetWord = data.targetWord || '?????';
        showStatus(`Game Over! The word was: ${targetWord.toUpperCase()}`, 'error');
        saveGameResult(targetWord, attempts, false);
    } catch (error) {
        console.error('Failed to fetch target word:', error);
        showStatus('Game Over! Better luck next time!', 'error');
        saveGameResult('?????', attempts, false);
    }
}

export async function checkHealth() {
    try {
        const response = await apiCheckHealth();
        const data     = await response.json();
        if (response.ok) console.log('Server is running. Active sessions:', data.activeSessions);
        else             console.error('Server health check failed');
    } catch (error) {
        console.error('Cannot connect to server:', error);
    }
}

// ============================================================
// Strategy / Suggestion
// ============================================================

export async function changeStrategy() {
    if (!state.currentGameId) return;
    const strategy = document.getElementById('strategySelector')?.value;
    if (!strategy) return;
    try {
        const response = await apiSetStrategy(state.currentGameId, strategy);
        if (response.ok) console.log('Strategy changed to:', _getStrategyDisplayName(strategy));
        else             console.error('Failed to change strategy');
    } catch (error) {
        console.error('Error changing strategy:', error);
    }
}

export async function getSuggestion() {
    if (!state.currentGameId) return;
    if (state.gameEnded) { showStatus('Game has ended', 'error'); return; }
    if (state.helpUsedCount >= state.MAX_HELP_COUNT) {
        showStatus('Maximum help requests reached (3/3)', 'error');
        return;
    }
    try {
        const response = await apiGetSuggestion(state.currentGameId);
        const data     = await response.json();
        if (response.ok && data.suggestion) {
            state.helpUsedCount++;
            updateHelpCounter();
            const suggestion = data.suggestion.toUpperCase();
            const inputs     = document.querySelectorAll('.letter-input');
            for (let i = 0; i < suggestion.length && i < inputs.length; i++) {
                inputs[i].value = suggestion[i];
                inputs[i].classList.add('filled');
            }
            switchMobileView('game');
        } else if (response.ok && !data.suggestion) {
            showStatus('No valid words remaining', 'error');
        } else {
            showStatus('Failed to get suggestion', 'error');
        }
    } catch (error) {
        showStatus('Error getting suggestion: ' + error.message, 'error');
    }
}

function _getStrategyDisplayName(strategy) {
    const names = {
        RANDOM:                  'Random Selection',
        ENTROPY:                 'Maximum Entropy',
        MOST_COMMON_LETTERS:     'Most Common Letters',
        MINIMISE_COLUMN_LENGTHS: 'Minimise Column Lengths',
        DICTIONARY_REDUCTION:    'Dictionary Reduction',
        BELLMAN_OPTIMAL:         'Bellman Optimal',
        BELLMAN_FULL_DICTIONARY: 'Bellman Full Dictionary',
    };
    return names[strategy] || strategy;
}

// ============================================================
// Session history & statistics
// ============================================================

function getGameHistory() {
    const h = sessionStorage.getItem('gameHistory');
    return h ? JSON.parse(h) : [];
}

function getGameStats() {
    const s = sessionStorage.getItem('gameStats');
    return s ? JSON.parse(s) : { total: 0, won: 0, lost: 0, wonGames: [] };
}

function saveGameResult(targetWord, attempts, won) {
    const history = getGameHistory();
    history.unshift({
        targetWord,
        attempts,
        won,
        timestamp:      new Date().toISOString(),
        guesses:        [...state.currentGameGuesses],
        wordLength:     state.currentWordLength,
        dictionarySize: state.currentDictionarySize,
    });
    sessionStorage.setItem('gameHistory', JSON.stringify(history));

    const stats = getGameStats();
    stats.total++;
    if (won) { stats.won++; stats.wonGames.push(attempts); } else { stats.lost++; }
    sessionStorage.setItem('gameStats', JSON.stringify(stats));

    updateHistoryDisplay();
    updateStats();
}

function updateHistoryDisplay() {
    const history   = getGameHistory();
    const container = document.getElementById('game-history');
    if (!container) return;

    if (history.length === 0) {
        container.innerHTML = '<p style="text-align:center;color:var(--text-secondary);padding:20px;">No games played yet</p>';
        return;
    }

    let html = `<table style="width:100%;border-collapse:collapse;font-size:0.9em;">
        <thead><tr style="border-bottom:1px solid var(--border-color);color:var(--text-secondary);text-align:left;">
            <th style="padding:8px;">Word</th><th style="padding:8px;">Attempts</th>
            <th style="padding:8px;text-align:right;">Result</th>
        </tr></thead><tbody>`;
    history.slice(0, 5).forEach(game => {
        html += `<tr style="border-bottom:1px solid var(--border-color);">
            <td style="padding:8px;font-family:var(--font-mono);font-weight:700;color:var(--text-primary);">${game.targetWord.toUpperCase()}</td>
            <td style="padding:8px;color:var(--text-secondary);">${game.attempts}</td>
            <td style="padding:8px;text-align:right;"><span style="color:${game.won ? 'var(--success)' : 'var(--error)'};font-weight:700;">${game.won ? 'WON' : 'LOST'}</span></td>
        </tr>`;
    });
    html += '</tbody></table>';
    container.innerHTML = html;
}

function updateStats() {
    const stats  = getGameStats();
    const setEl  = (id, val) => { const el = document.getElementById(id); if (el) el.textContent = String(val); };
    if (stats.total === 0) {
        ['stat-total','stat-won','stat-lost'].forEach(id => setEl(id, '0'));
        setEl('stat-winrate', '0%');
        ['stat-min','stat-max','stat-avg'].forEach(id => setEl(id, '-'));
        updateAttemptsDistribution();
        return;
    }
    const winRateVal = (stats.won / stats.total) * 100;
    setEl('stat-total',   stats.total);
    setEl('stat-won',     stats.won);
    setEl('stat-lost',    stats.lost);
    setEl('stat-winrate', (winRateVal === 100 ? '100' : winRateVal.toFixed(2)) + '%');
    if (stats.wonGames.length > 0) {
        setEl('stat-min', Math.min(...stats.wonGames));
        setEl('stat-max', Math.max(...stats.wonGames));
        setEl('stat-avg', (stats.wonGames.reduce((a,b) => a+b, 0) / stats.wonGames.length).toFixed(1));
    } else {
        ['stat-min','stat-max','stat-avg'].forEach(id => setEl(id, '-'));
    }
    updateAttemptsDistribution();
}

function updateAttemptsDistribution() {
    const stats     = getGameStats();
    const container = document.getElementById('attemptsDistribution');
    if (!container) return;
    if (stats.total === 0) {
        container.innerHTML = '<p style="text-align:center;color:var(--text-secondary);font-size:0.85em;">No games played yet</p>';
        return;
    }
    const dist = { 1:0, 2:0, 3:0, 4:0, 5:0, 6:0, fail: stats.lost };
    stats.wonGames.forEach(a => { if (a >= 1 && a <= 6) dist[a]++; });
    const maxCount = Math.max(...Object.values(dist));

    let html = '<div style="display:flex;flex-direction:column;gap:6px;">';
    for (let i = 1; i <= 6; i++) {
        const count = dist[i];
        const pct   = maxCount > 0 ? (count / maxCount) * 100 : 0;
        html += `<div style="display:flex;align-items:center;gap:8px;">
            <div style="min-width:12px;text-align:right;font-size:0.9em;font-weight:600;color:var(--text-primary);">${i}</div>
            <div style="flex:1;height:24px;background:rgba(106,170,100,0.2);border-radius:4px;overflow:hidden;position:relative;">
                <div style="height:100%;width:${pct}%;background:var(--correct);transition:width 0.3s ease;display:flex;align-items:center;justify-content:flex-end;padding-right:6px;">
                    ${count > 0 ? `<span style="font-size:0.85em;font-weight:600;color:white;">${count}</span>` : ''}
                </div>
                ${count === 0 ? `<div style="position:absolute;left:6px;top:50%;transform:translateY(-50%);font-size:0.85em;color:var(--text-secondary);">0</div>` : ''}
            </div>
        </div>`;
    }
    const failPct = maxCount > 0 ? (dist.fail / maxCount) * 100 : 0;
    html += `<div style="display:flex;align-items:center;gap:8px;">
        <div style="min-width:12px;text-align:right;font-size:0.9em;font-weight:600;color:var(--error);">\u2717</div>
        <div style="flex:1;height:24px;background:rgba(239,68,68,0.2);border-radius:4px;overflow:hidden;position:relative;">
            <div style="height:100%;width:${failPct}%;background:var(--error);transition:width 0.3s ease;display:flex;align-items:center;justify-content:flex-end;padding-right:6px;">
                ${dist.fail > 0 ? `<span style="font-size:0.85em;font-weight:600;color:white;">${dist.fail}</span>` : ''}
            </div>
            ${dist.fail === 0 ? `<div style="position:absolute;left:6px;top:50%;transform:translateY(-50%);font-size:0.85em;color:var(--text-secondary);">0</div>` : ''}
        </div>
    </div>`;
    html += '</div>';
    container.innerHTML = html;
}

export function exportSessionGamesToCSV() {
    const history = getGameHistory();
    if (history.length === 0) { console.log('No games to export'); return; }
    let csv = 'Game#,Target Word,Word Length,Dictionary Size,Result,Attempts,Timestamp,Guesses\n';
    [...history].reverse().forEach((game, index) => {
        const targetWord     = game.targetWord.toUpperCase();
        const wordLength     = game.wordLength || targetWord.length;
        const dictionarySize = game.dictionarySize || 'N/A';
        const result         = game.won ? 'WON' : 'LOST';
        const timestamp      = game.timestamp ? new Date(game.timestamp).toLocaleString() : 'N/A';
        const guessesStr     = (game.guesses || []).map(g => g.guess ? g.guess.toUpperCase() : '').join('; ');
        csv += `${index+1},${targetWord},${wordLength},${dictionarySize},${result},${game.attempts},"${timestamp}","${guessesStr}"\n`;
    });
    const now    = new Date();
    const dateStr = now.toISOString().slice(0, 10);
    const timeStr = now.toTimeString().slice(0, 8).replace(/:/g, '-');
    _triggerDownload(csv, `wordai-session-${dateStr}_${timeStr}.csv`, 'text/csv;charset=utf-8;');
}

function _triggerDownload(content, filename, mimeType) {
    const blob = new Blob([content], { type: mimeType });
    const link = document.createElement('a');
    const url  = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', filename);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}

// ============================================================
// Session viewer
// ============================================================

export function showSessionViewer() {
    if (window.location.hash !== '#/session') window.location.hash = '#/session';
    _renderSessionDetails();
}

export function hideSessionViewer() {
    if (window.location.hash !== '#/play') window.location.hash = '#/play';
}

function _renderSessionDetails() {
    const history    = getGameHistory();
    const stats      = getGameStats();
    const contentDiv = document.getElementById('sessionContent');
    if (!contentDiv) return;

    if (history.length === 0) {
        contentDiv.innerHTML = `<div style="text-align:center;padding:40px;color:#888;"><h3>No games played yet</h3><p>Start playing to see detailed session analytics!</p></div>`;
        return;
    }

    let html = `<div style="background:rgba(33,150,243,0.08);padding:20px;border-radius:12px;margin-bottom:20px;border:2px solid var(--accent-primary);">
        <h3 style="margin-top:0;color:var(--text-primary);">Overall Session Statistics</h3>
        <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:15px;margin-top:15px;">
            ${_statCard('Total Games', stats.total, 'var(--accent-primary)')}
            ${_statCard('Win Rate', (stats.total > 0 ? Math.round((stats.won/stats.total)*100) : 0) + '%', 'var(--success)')}
            ${_statCard('Wins', stats.won, 'var(--success)')}
            ${_statCard('Losses', stats.lost, 'var(--error)')}
        </div>`;
    if (stats.wonGames.length > 0) {
        const minA = Math.min(...stats.wonGames);
        const maxA = Math.max(...stats.wonGames);
        const avgA = (stats.wonGames.reduce((a,b) => a+b, 0) / stats.wonGames.length).toFixed(2);
        html += `<div style="display:grid;grid-template-columns:repeat(3,1fr);gap:15px;margin-top:15px;">
            ${_statCard('Best (Min)', minA, 'var(--success)')}
            ${_statCard('Average', avgA, 'var(--accent-primary)')}
            ${_statCard('Worst (Max)', maxA, 'var(--warning)')}
        </div>`;
    }
    html += '</div><h3 style="color:var(--text-primary);margin-top:30px;margin-bottom:15px;">Game Details</h3>';

    history.forEach((game, index) => {
        const gameNumber    = history.length - index;
        const formattedDate = new Date(game.timestamp).toLocaleString();
        const borderColor   = game.won ? 'var(--success-green)' : 'var(--error-red)';
        const badgeStyle    = game.won
            ? 'background:var(--success-green);color:white;'
            : 'background:var(--error-red);color:white;';
        html += `<div style="background:white;color:var(--bg-primary);border-radius:12px;padding:20px;margin-bottom:20px;box-shadow:0 4px 12px rgba(0,0,0,0.1);border-left:5px solid ${borderColor};max-width:100%;overflow:hidden;box-sizing:border-box;">
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:15px;">
                <div><h4 style="margin:0;color:var(--bg-primary);">Game #${gameNumber}: ${game.targetWord.toUpperCase()}</h4>
                    <div style="font-size:0.85em;color:#888;margin-top:5px;">${formattedDate}</div></div>
                <div style="text-align:right;">
                    <span style="display:inline-block;padding:8px 16px;border-radius:20px;font-weight:700;font-size:0.9em;${badgeStyle}">${game.won ? '\u2713 WON' : '\u2717 LOST'}</span>
                    <div style="margin-top:8px;font-size:1.2em;font-weight:700;color:var(--bg-primary);">${game.attempts} Attempts</div>
                </div>
            </div>`;

        if (game.guesses && game.guesses.length > 0) {
            html += `<div style="margin-top:20px;"><h5 style="color:var(--bg-primary);margin-bottom:10px;">Guess History:</h5>
                <div style="overflow-x:auto;-webkit-overflow-scrolling:touch;width:100%;">
                <table style="width:100%;min-width:340px;border-collapse:collapse;">
                    <thead><tr style="background:#f1f5f9;color:var(--bg-primary);font-size:0.8em;text-transform:uppercase;">
                        <th style="padding:6px 8px;text-align:left;border-bottom:2px solid #cbd5e1;white-space:nowrap;">#</th>
                        <th style="padding:6px 8px;text-align:left;border-bottom:2px solid #cbd5e1;white-space:nowrap;">Guess</th>
                        <th style="padding:6px 8px;text-align:left;border-bottom:2px solid #cbd5e1;white-space:nowrap;">Response</th>
                        <th style="padding:6px 8px;text-align:right;border-bottom:2px solid #cbd5e1;white-space:nowrap;">Rem.</th>
                        <th style="padding:6px 8px;text-align:right;border-bottom:2px solid #cbd5e1;white-space:nowrap;">Red.%</th>
                    </tr></thead><tbody>`;
            game.guesses.forEach(gd => {
                const redPct = game.dictionarySize > 0
                    ? (((game.dictionarySize - gd.remainingWords) / game.dictionarySize) * 100).toFixed(1) : 0;
                let responseHtml = '<div style="display:flex;gap:3px;">';
                (gd.results || []).forEach(r => {
                    const bg = r.status === 'G' ? '#6aaa64' : r.status === 'A' ? '#c9b458' : '#787c7e';
                    responseHtml += `<div style="width:24px;height:24px;background:${bg};color:white;display:flex;align-items:center;justify-content:center;border-radius:3px;font-weight:700;font-size:0.75em;font-family:var(--font-mono);">${r.letter}</div>`;
                });
                responseHtml += '</div>';
                html += `<tr style="border-bottom:1px solid #e2e8f0;color:var(--bg-primary);">
                    <td style="padding:6px 8px;font-weight:700;white-space:nowrap;">#${gd.attempt}</td>
                    <td style="padding:6px 8px;font-family:var(--font-mono);font-weight:700;font-size:1em;white-space:nowrap;">${gd.guess.toUpperCase()}</td>
                    <td style="padding:6px 8px;">${responseHtml}</td>
                    <td style="padding:6px 8px;text-align:right;font-family:var(--font-mono);font-weight:600;white-space:nowrap;">${gd.remainingWords || 'N/A'}</td>
                    <td style="padding:6px 8px;text-align:right;font-family:var(--font-mono);font-weight:600;color:var(--success-green);white-space:nowrap;">${redPct}%</td>
                </tr>`;
                if (gd.dictionaryMetrics) {
                    html += `<tr style="background:rgba(33,150,243,0.05);"><td colspan="5" style="padding:6px 8px;font-size:0.8em;word-break:break-word;overflow-wrap:anywhere;">
                        <strong>Metrics:</strong> Unique: ${gd.dictionaryMetrics.uniqueCharacters||'N/A'} |
                        Letters: ${gd.dictionaryMetrics.letterCount||'N/A'}${gd.dictionaryMetrics.columnLengths ? ' | Cols: ['+gd.dictionaryMetrics.columnLengths.join(', ')+']' : ''}
                    </td></tr>`;
                }
            });
            html += '</tbody></table></div></div>';
        } else {
            html += '<div style="color:#888;font-style:italic;margin-top:10px;">Detailed guess data not available for this game.</div>';
        }
        html += '</div>';
    });

    contentDiv.innerHTML = html;
}

function _statCard(label, value, color) {
    return `<div style="background:white;padding:15px;border-radius:8px;text-align:center;box-shadow:0 2px 8px rgba(0,0,0,0.1);">
        <div style="font-size:0.8em;color:var(--bg-tertiary);font-weight:700;text-transform:uppercase;margin-bottom:8px;">${label}</div>
        <div style="font-size:2em;font-weight:800;color:${color};font-family:var(--font-mono);">${value}</div>
    </div>`;
}

// ============================================================
// Dictionary management
// ============================================================

async function loadDictionaries() {
    try {
        const response = await apiGetDictionaries();
        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
        state.availableDictionaries = await response.json();
        populateDictionarySelector();
    } catch (error) {
        console.error('Failed to load dictionaries:', error);
        ['dictionarySelector', 'dictionarySelectorDict'].forEach(id => {
            const el = document.getElementById(id);
            if (el) el.innerHTML = '<option value="" disabled>Unable to load dictionaries</option>';
        });
    }
}

function populateDictionarySelector() {
    const selector     = document.getElementById('dictionarySelector');
    const selectorDict = document.getElementById('dictionarySelectorDict');

    if (selector)     selector.innerHTML     = '';
    if (selectorDict) selectorDict.innerHTML = '';

    state.availableDictionaries.forEach(dict => {
        [selector, selectorDict].forEach(sel => {
            if (!sel) return;
            const opt = document.createElement('option');
            opt.value       = dict.id;
            opt.textContent = dict.name + (dict.available ? '' : ' (Not Available)');
            if (!dict.available) opt.disabled = true;
            if (dict.description) opt.title   = dict.description;
            sel.appendChild(opt);
        });
    });

    const defaultDict = state.availableDictionaries.find(d => d.available && d.wordLength === 5)
        || state.availableDictionaries.find(d => d.available);
    if (defaultDict) {
        if (selector)     selector.value     = defaultDict.id;
        if (selectorDict) selectorDict.value = defaultDict.id;
        adjustLetterInputGrid(defaultDict.wordLength);
    }

    // Autoplay word-length selector
    const autoplayWordLength = document.getElementById('autoplayWordLength');
    if (autoplayWordLength) {
        const currentVal    = autoplayWordLength.value;
        autoplayWordLength.innerHTML = '';
        const uniqueLengths = [...new Set(state.availableDictionaries.filter(d => d.available).map(d => d.wordLength))].sort((a,b) => a-b);
        uniqueLengths.forEach(len => {
            const opt = document.createElement('option');
            opt.value = String(len); opt.textContent = `${len} Letters`;
            autoplayWordLength.appendChild(opt);
        });
        const defaultLen = uniqueLengths.includes(5) ? '5' : String(uniqueLengths[0] || '');
        autoplayWordLength.value = uniqueLengths.includes(parseInt(currentVal)) ? currentVal : defaultLen;
    }

    // Bot performance analysis dictionary selector
    const analysisSelect = document.getElementById('analysisDictionary');
    if (analysisSelect) {
        analysisSelect.innerHTML = '';
        state.availableDictionaries.filter(d => d.available).forEach(dict => {
            const opt = document.createElement('option');
            opt.value = dict.id; opt.textContent = dict.name;
            if (dict.description) opt.title = dict.description;
            analysisSelect.appendChild(opt);
        });
        const df = state.availableDictionaries.find(d => d.available && d.wordLength === 5)
            || state.availableDictionaries.find(d => d.available);
        if (df) analysisSelect.value = df.id;
    }

    // Bot demo dictionary selector (filtered by word length)
    if (document.getElementById('autoplayWordLength') && document.getElementById('autoplayDictionary')) {
        filterAutoplayDictionaries();
    }

    if (state.currentView === 'dictionary') refreshDictionaryScreen();
}

function onDictionaryChange() {
    const selector     = document.getElementById('dictionarySelector');
    const selectorDict = document.getElementById('dictionarySelectorDict');

    let selectedId = null;
    if (state.currentView === 'dictionary' && selectorDict?.value) {
        selectedId = selectorDict.value;
        if (selector) selector.value = selectedId;
    } else if (selector?.value) {
        selectedId = selector.value;
        if (selectorDict) selectorDict.value = selectedId;
    }

    const selectedDict = state.availableDictionaries.find(d => d.id === selectedId);
    if (!selectedDict) return;

    adjustLetterInputGrid(selectedDict.wordLength);

    if (state.currentView === 'dictionary') {
        state.dictionaryScreenState.dictionaryId = null;
        refreshDictionaryScreen();
    } else if (state.currentView === 'play') {
        newGame().catch(err => console.error('Failed to start new game after dictionary change:', err));
    }
}

// Kick off dictionary load as soon as the module is evaluated.
// type="module" scripts are implicitly deferred, so the DOM is ready.
loadDictionaries();
