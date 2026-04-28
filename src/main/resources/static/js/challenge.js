import { state } from './state.js';
import {
    apiCreateChallenge,
    apiGetChallengeState,
    apiMakeChallengeGuess,
    apiPauseChallenge,
    apiSkipChallenge,
    apiUseChallengeAssist,
} from './api.js';
import { shouldSuppressNativeKeyboard, showStatus } from './ui.js';
import { getBrowserSessionId } from './browser-session.js';

const CHALLENGE_STORAGE_KEY = 'wordaiActiveChallengeId';
export function initializeChallengeUI() {
    createChallengeInputs(state.currentWordLength || 5);
    initializeChallengeKeyboard();
    attachChallengeKeyboardEvents();
    document.getElementById('challengeDictionarySelector')?.addEventListener('change', event => {
        const nextWordLength = resolveChallengeWordLength(event.target.value);
        state.currentWordLength = nextWordLength;
        createChallengeInputs(nextWordLength);
        renderChallengeBoard();
    });
    restoreChallengeSession();
    renderChallengeView();
}

export function handleChallengeViewActivated() {
    const selector = document.getElementById('challengeDictionarySelector');
    if (selector && selector.options.length === 0 && typeof window.populateDictionarySelector === 'function') {
        window.populateDictionarySelector();
    }

    // Always cancel any stale countdown left over from navigating away mid-transition.
    // If a fresh challenge is still active, refreshChallengeState will re-sync the timer.
    dismissInterPuzzleOverlay();

    if (state.challenge.currentChallengeId) {
        refreshChallengeState();
    } else {
        renderChallengeView();
    }
}

export async function startChallenge() {
    const startBtn = document.getElementById('startChallengeBtn');
    try {
        // Reset the full client-side lifecycle before creating a replacement run.
        // This clears any stale summary, guesses, overlay state, and persisted ID
        // so the new challenge always starts from a clean slate.
        resetChallengeState();
        renderChallengeView();
        if (startBtn) startBtn.disabled = true;

        const selector = document.getElementById('challengeDictionarySelector');
        const dictionaryId = selector?.value || null;
        state.currentWordLength = resolveChallengeWordLength(dictionaryId);
        createChallengeInputs(state.currentWordLength);
        const response = await apiCreateChallenge({
            dictionaryId,
            browserSessionId: getBrowserSessionId(),
        });
        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.message || 'Failed to create challenge');
        }

        resetChallengePuzzleState();
        applyChallengeSnapshot(data, { resetPuzzle: true });
        persistChallengeId(data.challengeId);
        showStatus(data.message || 'Challenge created.', 'success');
    } catch (error) {
        console.error('Failed to start challenge:', error);
        showStatus(error.message || 'Failed to start challenge.', 'error');
        // Re-render so the left panel reflects the null currentChallengeId
        // (e.g. if the API failed after we cleared the ID above).
        renderChallengeView();
    } finally {
        if (startBtn) startBtn.disabled = false;
    }
}

export async function submitChallengeGuess() {
    if (!state.challenge.currentChallengeId || state.challenge.challengeComplete || state.challenge.challengeFailed) {
        return;
    }

    const word = getChallengeGuess();
    if (!word) {
        showStatus('Please enter a word.', 'error', { clearOnInput: true });
        return;
    }

    const expectedLength = getChallengeInputs().length;
    if (word.length !== expectedLength) {
        showStatus(`Word must be ${expectedLength} letters long.`, 'error', { clearOnInput: true });
        return;
    }

    try {
        const previousPuzzle = state.challenge.currentPuzzleNumber;
        const response = await apiMakeChallengeGuess(state.challenge.currentChallengeId, word);
        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.message || 'Failed to submit challenge guess');
        }

        applyChallengeSnapshot(data, {
            appendLastGuess: true,
            previousPuzzleNumber: previousPuzzle,
        });
        clearChallengeInputs();
        if (data.challengeComplete) {
            showStatus(data.message || 'Challenge completed!', 'success', {
                celebrate: true,
                autoHideMs: 4500,
            });
        } else if (data.lastGuess?.puzzleSolved) {
            showStatus('Congratulations! You won!', 'success', {
                celebrate: true,
                autoHideMs: 4500,
            });
        } else if (data.message) {
            const toastType = data.challengeFailed ? 'error' : 'info';
            showStatus(data.message, toastType);
        }
    } catch (error) {
        console.error('Failed to submit challenge guess:', error);
        showStatus(error.message || 'Challenge guess failed.', 'error', { clearOnInput: true });
    }
}

export async function useChallengeAssist() {
    if (!state.challenge.currentChallengeId) return;
    try {
        const response = await apiUseChallengeAssist(
            state.challenge.currentChallengeId,
            state.challenge.selectedStrategy,
        );
        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.message || 'Failed to use challenge assist');
        }

        applyChallengeSnapshot(data);
        if (data.suggestedWord) {
            fillChallengeInputs(data.suggestedWord);
        }
    } catch (error) {
        console.error('Failed to use challenge assist:', error);
        showStatus(error.message || 'Challenge assist failed.', 'error');
    }
}

export async function pauseChallenge() {
    if (!state.challenge.currentChallengeId) return;
    try {
        const response = await apiPauseChallenge(state.challenge.currentChallengeId);
        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.message || 'Failed to pause challenge');
        }

        applyChallengeSnapshot(data);
        showStatus(data.message || 'Pause applied.', 'success');
    } catch (error) {
        console.error('Failed to pause challenge:', error);
        showStatus(error.message || 'Pause failed.', 'error');
    }
}

export async function skipChallenge() {
    if (!state.challenge.currentChallengeId) return;
    try {
        const response = await apiSkipChallenge(state.challenge.currentChallengeId);
        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.message || 'Failed to skip puzzle');
        }

        resetChallengePuzzleState();
        applyChallengeSnapshot(data, { resetPuzzle: true });
        showStatus(data.message || 'Puzzle skipped.', 'warning');
    } catch (error) {
        console.error('Failed to skip challenge puzzle:', error);
        showStatus(error.message || 'Skip failed.', 'error');
    }
}

export function changeChallengeStrategy() {
    const selector = document.getElementById('challengeStrategySelector');
    state.challenge.selectedStrategy = selector?.value || 'ENTROPY';
}

async function refreshChallengeState() {
    if (!state.challenge.currentChallengeId) return;
    const challengeIdAtCallTime = state.challenge.currentChallengeId;
    try {
        const response = await apiGetChallengeState(challengeIdAtCallTime);
        // Discard if a new challenge was started while this request was in-flight
        if (state.challenge.currentChallengeId !== challengeIdAtCallTime) return;
        if (response.status === 404) {
            clearPersistedChallengeId();
            resetChallengeState();
            renderChallengeView();
            return;
        }

        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.message || 'Failed to refresh challenge');
        }

        // Second guard: re-check after await in case challenge changed while awaiting json()
        if (state.challenge.currentChallengeId !== challengeIdAtCallTime) return;
        applyChallengeSnapshot(data);
    } catch (error) {
        console.error('Failed to refresh challenge state:', error);
        if (state.challenge.currentChallengeId === challengeIdAtCallTime) {
            resetChallengeState();
            renderChallengeView();
            showStatus('Challenge session could not be restored. Start a new challenge.', 'warning');
        }
    }
}

function restoreChallengeSession() {
    const challengeId = sessionStorage.getItem(CHALLENGE_STORAGE_KEY);
    if (!challengeId) return;
    state.challenge.currentChallengeId = challengeId;
    refreshChallengeState();
}

function applyChallengeSnapshot(snapshot, options = {}) {
    const previousPuzzleNumber = options.previousPuzzleNumber ?? state.challenge.currentPuzzleNumber;
    const puzzleChanged = options.resetPuzzle || (snapshot.currentPuzzleNumber !== previousPuzzleNumber);
    const isInterPuzzleTransition = puzzleChanged && !options.resetPuzzle && !!options.appendLastGuess
        && !snapshot.challengeComplete && !snapshot.challengeFailed
        && (snapshot.currentPuzzleNumber ?? 1) > 1;
    const resolvedWordLength = resolveChallengeWordLength(snapshot.dictionaryId);
    if (resolvedWordLength !== state.currentWordLength) {
        state.currentWordLength = resolvedWordLength;
        createChallengeInputs(resolvedWordLength);
    }

    state.challenge.currentChallengeId = snapshot.challengeId;
    state.challenge.dictionaryId = snapshot.dictionaryId;
    state.challenge.status = snapshot.status || 'ACTIVE';
    state.challenge.totalScore = snapshot.totalScore ?? 0;
    state.challenge.totalPuzzles = snapshot.totalPuzzles ?? 10;
    state.challenge.currentPuzzleNumber = snapshot.currentPuzzleNumber ?? 1;
    state.challenge.puzzlesCompleted = snapshot.puzzlesCompleted ?? 0;
    state.challenge.currentPuzzleTimeLimitSeconds = snapshot.currentPuzzleTimeLimitSeconds ?? 120;
    state.challenge.secondsRemaining = snapshot.secondsRemaining ?? 0;
    state.challenge.currentPuzzleAssistsRemaining = snapshot.currentPuzzleAssistsRemaining ?? 0;
    state.challenge.currentAttempts = snapshot.currentAttempts ?? 0;
    state.challenge.maxAttempts = snapshot.maxAttempts ?? 6;
    state.challenge.pauseUsed = !!snapshot.pauseUsed;
    state.challenge.skipUsed = !!snapshot.skipUsed;
    state.challenge.challengeComplete = !!snapshot.challengeComplete;
    state.challenge.challengeFailed = !!snapshot.challengeFailed;
    state.challenge.message = snapshot.message || '';
    state.challenge.suggestedWord = snapshot.suggestedWord || null;
    state.challenge.revealedTargetWord = snapshot.revealedTargetWord || null;
    state.challenge.completedPuzzles = Array.isArray(snapshot.completedPuzzles) ? snapshot.completedPuzzles : [];
    state.challenge.lastGuess = snapshot.lastGuess || null;

    if (snapshot.challengeId) {
        persistChallengeId(snapshot.challengeId);
    }

    if (puzzleChanged) {
        resetChallengePuzzleState();
    }

    if (options.appendLastGuess && snapshot.lastGuess && (!puzzleChanged || snapshot.challengeComplete || snapshot.challengeFailed)) {
        appendChallengeGuess(snapshot.lastGuess);
    }

    if (snapshot.suggestedWord) {
        fillChallengeInputs(snapshot.suggestedWord);
    }

    if (snapshot.challengeComplete || snapshot.challengeFailed) {
        stopChallengeTimer();
        dismissInterPuzzleOverlay();
        renderChallengeView();
    } else if (isInterPuzzleTransition) {
        stopChallengeTimer();
        renderChallengeView();
        const snapshotTimestamp = Date.now();
        startInterPuzzleCountdown(
            snapshot.currentPuzzleNumber,
            snapshot.totalPuzzles ?? 10,
            snapshot.secondsRemaining ?? 0,
            snapshotTimestamp,
        );
    } else {
        syncChallengeTimer(snapshot.secondsRemaining ?? 0);
        renderChallengeView();
    }
}

function dismissInterPuzzleOverlay() {
    if (state.challenge.countdownIntervalId) {
        window.clearInterval(state.challenge.countdownIntervalId);
        state.challenge.countdownIntervalId = null;
    }
    if (state.challenge.countdownTimeoutId) {
        window.clearTimeout(state.challenge.countdownTimeoutId);
        state.challenge.countdownTimeoutId = null;
    }
    const overlay = document.getElementById('challengeNextOverlay');
    if (overlay) overlay.hidden = true;
    getChallengeInputs().forEach(input => { input.disabled = false; });
}

function startInterPuzzleCountdown(newPuzzleNumber, totalPuzzles, snapshotSecondsRemaining, snapshotTimestamp) {
    dismissInterPuzzleOverlay();

    const overlay = document.getElementById('challengeNextOverlay');
    const numEl = document.getElementById('challengeNextNum');
    const countEl = document.getElementById('challengeNextCountdown');
    const strip = document.getElementById('challengeGameStrip');

    function completeCountdown() {
        dismissInterPuzzleOverlay();
        const suppressKeyboard = shouldSuppressNativeKeyboard();
        if (!suppressKeyboard) getChallengeInputs()[0]?.focus();
        const elapsed = Math.floor((Date.now() - snapshotTimestamp) / 1000);
        syncChallengeTimer(Math.max(0, snapshotSecondsRemaining - elapsed));
        renderChallengeView();
    }

    if (!overlay) {
        completeCountdown();
        return;
    }

    if (numEl) numEl.textContent = `${newPuzzleNumber}/${totalPuzzles}`;
    if (countEl) countEl.textContent = '5';
    if (strip) strip.hidden = true;
    overlay.hidden = false;
    getChallengeInputs().forEach(input => { input.disabled = true; });

    let count = 4;
    state.challenge.countdownIntervalId = window.setInterval(() => {
        if (count > 0) {
            if (countEl) countEl.textContent = String(count);
            count--;
        } else {
            window.clearInterval(state.challenge.countdownIntervalId);
            state.challenge.countdownIntervalId = null;
            if (countEl) countEl.textContent = 'GO!';
            state.challenge.countdownTimeoutId = window.setTimeout(completeCountdown, 700);
        }
    }, 1000);
}

function appendChallengeGuess(lastGuess) {
    const duplicate = state.challenge.currentPuzzleGuesses.find(entry =>
        entry.guessedWord === lastGuess.guessedWord && entry.attemptNumber === lastGuess.attemptNumber,
    );
    if (duplicate) return;

    state.challenge.currentPuzzleGuesses.push(lastGuess);
    updateChallengeKeyboardStatus(lastGuess.results || []);
}

function updateChallengeKeyboardStatus(results) {
    results.forEach(result => {
        const letter = (result.letter || '').toUpperCase();
        if (!letter) return;

        const current = state.challenge.keyboardStatusMap[letter];
        if (result.status === 'G') {
            state.challenge.keyboardStatusMap[letter] = 'G';
        } else if (result.status === 'A' && current !== 'G') {
            state.challenge.keyboardStatusMap[letter] = 'A';
        } else if ((result.status === 'R' || result.status === 'X') && !current) {
            state.challenge.keyboardStatusMap[letter] = 'R';
        }
    });
}

function createChallengeInputs(wordLength) {
    const host = document.getElementById('challengeLetterInputs');
    if (!host) return;
    host.innerHTML = '';
    const suppressKeyboard = shouldSuppressNativeKeyboard();

    for (let index = 0; index < wordLength; index++) {
        const input = document.createElement('input');
        input.type = 'text';
        input.id = `challengeLetter${index}`;
        input.maxLength = 1;
        input.readOnly = suppressKeyboard;
        input.inputMode = suppressKeyboard ? 'none' : 'text';
        input.autocomplete = 'off';
        input.autocapitalize = suppressKeyboard ? 'off' : 'characters';
        input.setAttribute('autocorrect', 'off');
        input.spellcheck = false;
        input.className = 'letter-input challenge-letter-input';
        input.setAttribute('aria-label', `Challenge letter ${index + 1} of ${wordLength}`);
        host.appendChild(input);
    }
}

function initializeChallengeKeyboard() {
    const keyboard = document.getElementById('challengeKeyboard');
    if (!keyboard) return;
    keyboard.innerHTML = '';

    const rows = [
        ['Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P'],
        ['A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L'],
        ['Z', 'X', 'C', 'V', 'B', 'N', 'M'],
    ];

    rows.forEach((row, rowIndex) => {
        const rowEl = document.createElement('div');
        rowEl.className = 'keyboard-row';

        if (rowIndex === 2) {
            rowEl.appendChild(createChallengeActionKey('ENTER', 'game-key wide-key enter-key', submitChallengeGuess, 'challengeGuessBtn'));
        }

        row.forEach(letter => {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'game-key key-unused';
            button.textContent = letter;
            button.dataset.letter = letter;
            button.setAttribute('aria-label', `Letter ${letter}`);
            button.addEventListener('click', () => insertChallengeLetter(letter));
            rowEl.appendChild(button);
        });

        if (rowIndex === 2) {
            rowEl.appendChild(createChallengeActionKey('⌫', 'game-key wide-key backspace-key', deleteChallengeLetter));
        }

        keyboard.appendChild(rowEl);
    });
}

function createChallengeActionKey(label, className, onClick, id = '') {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = className;
    button.textContent = label;
    if (id) button.id = id;
    button.addEventListener('click', onClick);
    return button;
}

function attachChallengeKeyboardEvents() {
    document.addEventListener('keydown', event => {
        if (state.currentView !== 'challenge') return;
        if (!state.challenge.currentChallengeId || state.challenge.challengeComplete || state.challenge.challengeFailed) return;

        const activeElement = document.activeElement;
        const tagName = activeElement?.tagName || '';
        if ((tagName === 'INPUT' || tagName === 'TEXTAREA' || tagName === 'SELECT')
                && !activeElement.classList.contains('challenge-letter-input')) {
            return;
        }

        if (event.key === 'Enter') {
            event.preventDefault();
            submitChallengeGuess();
            return;
        }
        if (event.key === 'Backspace') {
            event.preventDefault();
            deleteChallengeLetter();
            return;
        }
        if (/^[a-zA-Z]$/.test(event.key)) {
            event.preventDefault();
            insertChallengeLetter(event.key.toUpperCase());
        }
    });
}

function insertChallengeLetter(letter) {
    if (state.challenge.challengeComplete || state.challenge.challengeFailed) return;
    const inputs = getChallengeInputs();
    for (const input of inputs) {
        if (!input.value) {
            input.value = letter;
            input.classList.add('filled');
            break;
        }
    }
}

function deleteChallengeLetter() {
    const inputs = getChallengeInputs();
    for (let index = inputs.length - 1; index >= 0; index--) {
        if (inputs[index].value) {
            inputs[index].value = '';
            inputs[index].classList.remove('filled');
            break;
        }
    }
}

function getChallengeGuess() {
    return Array.from(getChallengeInputs())
        .map(input => input.value)
        .join('')
        .trim()
        .toLowerCase();
}

function clearChallengeInputs() {
    const suppressKeyboard = shouldSuppressNativeKeyboard();
    getChallengeInputs().forEach(input => {
        input.value = '';
        input.classList.remove('filled');
        if (!suppressKeyboard) input.disabled = false;
    });

    if (!suppressKeyboard) {
        getChallengeInputs()[0]?.focus();
    }
}

function fillChallengeInputs(word) {
    const letters = (word || '').toUpperCase().split('');
    const inputs = getChallengeInputs();
    inputs.forEach((input, index) => {
        input.value = letters[index] || '';
        input.classList.toggle('filled', !!letters[index]);
    });
}

function getChallengeInputs() {
    return document.querySelectorAll('#challengeLetterInputs .challenge-letter-input');
}

function syncChallengeTimer(secondsRemaining) {
    stopChallengeTimer();
    state.challenge.syncedSecondsRemaining = secondsRemaining;
    state.challenge.timerSyncedAtMs = Date.now();
    updateChallengeCountdown();

    state.challenge.timerIntervalId = window.setInterval(() => {
        updateChallengeCountdown();
        if (state.challenge.secondsRemaining <= 0) {
            stopChallengeTimer();
            refreshChallengeState();
        }
    }, 1000);
}

function updateChallengeCountdown() {
    const elapsedSeconds = Math.floor((Date.now() - state.challenge.timerSyncedAtMs) / 1000);
    state.challenge.secondsRemaining = Math.max(0, state.challenge.syncedSecondsRemaining - elapsedSeconds);
    const secs = state.challenge.secondsRemaining;
    const isDanger = secs <= 10;

    // Game panel strip timer
    const stripTimer = document.getElementById('challengeGameStripTimer');
    if (stripTimer) {
        stripTimer.textContent = `${secs}s`;
        stripTimer.classList.toggle('danger', isDanger);
    }

    // Timer fill bar
    const total = state.challenge.currentPuzzleTimeLimitSeconds || 120;
    const pct = total > 0 ? (secs / total) * 100 : 0;
    const fill = document.getElementById('challengeTimerFill');
    if (fill) {
        fill.style.width = `${pct}%`;
        fill.classList.toggle('danger', isDanger);
    }
}

function stopChallengeTimer() {
    if (state.challenge.timerIntervalId) {
        window.clearInterval(state.challenge.timerIntervalId);
        state.challenge.timerIntervalId = null;
    }
}

function resetChallengeState() {
    stopChallengeTimer();
    clearPersistedChallengeId();
    state.challenge.currentChallengeId = null;
    state.challenge.dictionaryId = null;
    state.challenge.status = 'IDLE';
    state.challenge.totalScore = 0;
    state.challenge.currentPuzzleNumber = 1;
    state.challenge.puzzlesCompleted = 0;
    state.challenge.currentPuzzleTimeLimitSeconds = 120;
    state.challenge.secondsRemaining = 120;
    state.challenge.currentPuzzleAssistsRemaining = 3;
    state.challenge.currentAttempts = 0;
    state.challenge.maxAttempts = 6;
    state.challenge.pauseUsed = false;
    state.challenge.skipUsed = false;
    state.challenge.challengeComplete = false;
    state.challenge.challengeFailed = false;
    state.challenge.message = '';
    state.challenge.suggestedWord = null;
    state.challenge.revealedTargetWord = null;
    state.challenge.lastGuess = null;
    state.challenge.completedPuzzles = [];
    state.challenge.syncedSecondsRemaining = 120;
    state.challenge.timerSyncedAtMs = 0;
    resetChallengePuzzleState();
}

function resetChallengePuzzleState() {
    dismissInterPuzzleOverlay();
    state.challenge.currentPuzzleGuesses = [];
    state.challenge.keyboardStatusMap = {};
    clearChallengeInputs();
}

function persistChallengeId(challengeId) {
    sessionStorage.setItem(CHALLENGE_STORAGE_KEY, challengeId);
}

function clearPersistedChallengeId() {
    sessionStorage.removeItem(CHALLENGE_STORAGE_KEY);
}

function renderChallengeView() {
    // Defensive lifecycle sync: a missing challenge session means all transient UI
    // for an inter-puzzle transition must be torn down immediately.
    if (!state.challenge.currentChallengeId) {
        dismissInterPuzzleOverlay();
        stopChallengeTimer();
    } else if (!state.challenge.countdownIntervalId && !state.challenge.countdownTimeoutId) {
        const overlay = document.getElementById('challengeNextOverlay');
        if (overlay) overlay.hidden = true;
    }
    renderChallengeSummary();
    renderChallengeBoard();
    renderCompletedPuzzleList();
    updateChallengeControls();
}

function renderChallengeSummary() {
    const emptyState = document.getElementById('challengeEmptyState');
    const activeState = document.getElementById('challengeActiveState');
    const statusPill = document.getElementById('challengeStatusPill');
    const scoreValue = document.getElementById('challengeScoreValue');
    const puzzleValue = document.getElementById('challengePuzzleValue');
    const assistValue = document.getElementById('challengeAssistValue');
    const timerValue = document.getElementById('challengeTimerValue');
    const message = document.getElementById('challengeInlineMessage');

    const hasChallenge = !!state.challenge.currentChallengeId;
    if (emptyState) emptyState.hidden = hasChallenge;
    if (activeState) activeState.hidden = !hasChallenge;
    if (!hasChallenge) {
        if (message) message.textContent = 'Start a new challenge to begin your 10-puzzle run.';
        return;
    }

    if (statusPill) {
        statusPill.textContent = state.challenge.status.replaceAll('_', ' ');
        statusPill.className = `challenge-status-pill status-${state.challenge.status.toLowerCase()}`;
    }
    if (scoreValue) scoreValue.textContent = String(state.challenge.totalScore);
    if (puzzleValue) puzzleValue.textContent = `${state.challenge.currentPuzzleNumber}/${state.challenge.totalPuzzles}`;
    if (assistValue) assistValue.textContent = String(state.challenge.currentPuzzleAssistsRemaining);
    if (timerValue) {
        timerValue.textContent = `${state.challenge.secondsRemaining}s`;
        timerValue.classList.toggle('danger', state.challenge.secondsRemaining <= 10);
    }

    // Game panel strip
    const isFinished = state.challenge.challengeComplete || state.challenge.challengeFailed;
    const gameStrip = document.getElementById('challengeGameStrip');
    if (gameStrip) gameStrip.hidden = !hasChallenge || isFinished;
    const stripPuzzle = document.getElementById('challengeGameStripPuzzle');
    if (stripPuzzle) stripPuzzle.textContent = `${state.challenge.currentPuzzleNumber}/${state.challenge.totalPuzzles}`;
    const stripTimer = document.getElementById('challengeGameStripTimer');
    if (stripTimer) {
        stripTimer.textContent = `${state.challenge.secondsRemaining}s`;
        stripTimer.classList.toggle('danger', state.challenge.secondsRemaining <= 10);
    }
    const total = state.challenge.currentPuzzleTimeLimitSeconds || 120;
    const pct = total > 0 ? (state.challenge.secondsRemaining / total) * 100 : 0;
    const fill = document.getElementById('challengeTimerFill');
    if (fill) {
        fill.style.width = `${pct}%`;
        fill.classList.toggle('danger', state.challenge.secondsRemaining <= 10);
    }
    if (message) {
        const base = state.challenge.message || 'Solve the current puzzle before the timer expires.';
        if (state.challenge.challengeComplete && state.challenge.revealedTargetWord) {
            message.textContent = `${base} Final target: ${state.challenge.revealedTargetWord.toUpperCase()}.`;
        } else if (state.challenge.challengeFailed && state.challenge.revealedTargetWord) {
            message.textContent = `${base} Final target: ${state.challenge.revealedTargetWord.toUpperCase()}.`;
        } else {
            message.textContent = base;
        }
    }
}

function renderChallengeBoard() {
    const board = document.getElementById('challengeGuessHistory');
    const attemptsValue = document.getElementById('challengeAttempts');
    const maxAttemptsValue = document.getElementById('challengeMaxAttempts');
    if (!board) return;

    if (attemptsValue) attemptsValue.textContent = String(state.challenge.currentAttempts);
    if (maxAttemptsValue) maxAttemptsValue.textContent = String(state.challenge.maxAttempts);

    board.innerHTML = '';
    for (let rowIndex = 0; rowIndex < state.challenge.maxAttempts; rowIndex++) {
        const row = document.createElement('div');
        row.className = 'guess-row challenge-board-row';

        const guess = state.challenge.currentPuzzleGuesses[rowIndex];
        if (guess && Array.isArray(guess.results)) {
            guess.results.forEach(result => {
                const cell = createChallengeCell(result.letter, result.status);
                row.appendChild(cell);
            });
        } else {
            for (let index = 0; index < state.currentWordLength; index++) {
                row.appendChild(createChallengeCell('', ''));
            }
        }
        board.appendChild(row);
    }
    renderChallengeKeyboard();
}

function renderChallengeKeyboard() {
    document.querySelectorAll('#challengeKeyboard .game-key[data-letter]').forEach(button => {
        const letter = button.dataset.letter;
        const status = state.challenge.keyboardStatusMap[letter];
        button.className = 'game-key';
        if (status === 'G') button.classList.add('key-correct');
        else if (status === 'A') button.classList.add('key-present');
        else if (status === 'R') button.classList.add('key-absent');
        else button.classList.add('key-unused');
    });
}

function createChallengeCell(letter, status) {
    const cell = document.createElement('div');
    cell.className = 'letter-box challenge-board-cell';
    cell.textContent = letter || '';
    if (status === 'G') cell.classList.add('letter-correct');
    else if (status === 'A') cell.classList.add('letter-present');
    else if (status === 'R' || status === 'X') cell.classList.add('letter-absent');
    return cell;
}

function renderCompletedPuzzleList() {
    const host = document.getElementById('challengeCompletedList');
    if (!host) return;

    const total = state.challenge.totalPuzzles || 10;
    const completedMap = new Map(
        state.challenge.completedPuzzles.map(p => [p.puzzleNumber, p]),
    );

    const rows = [];
    for (let index = 1; index <= total; index++) {
        const summary = completedMap.get(index);
        if (summary) {
            rows.push(`
                <article class="challenge-summary-item status-${summary.status.toLowerCase()}">
                    <div class="challenge-summary-top">
                        <strong>Puzzle ${summary.puzzleNumber}</strong>
                        <span>${summary.status.replaceAll('_', ' ')}</span>
                    </div>
                    <div class="challenge-summary-bottom">
                        <span>${summary.targetWord?.toUpperCase() || '?????'}</span>
                        <span>${summary.scoreAwarded > 0 ? '+' : ''}${summary.scoreAwarded} pts</span>
                        <span>${summary.attemptsUsed}/${summary.maxAttempts} attempts</span>
                    </div>
                </article>
            `);
        } else {
            rows.push(`
                <article class="challenge-summary-item status-pending">
                    <div class="challenge-summary-top">
                        <strong>Puzzle ${index}</strong>
                        <span class="challenge-muted">&mdash;</span>
                    </div>
                    <div class="challenge-summary-bottom challenge-muted">
                        <span>&mdash;</span>
                        <span>&mdash;</span>
                        <span>&mdash;</span>
                    </div>
                </article>
            `);
        }
    }
    host.innerHTML = rows.join('');
}

function updateChallengeControls() {
    const hasChallenge = !!state.challenge.currentChallengeId;
    const inactive = !hasChallenge || state.challenge.challengeComplete || state.challenge.challengeFailed;

    const startBtn = document.getElementById('startChallengeBtn');
    const guessBtn = document.getElementById('challengeGuessBtn');
    const assistBtn = document.getElementById('challengeAssistBtn');
    const pauseBtn = document.getElementById('challengePauseBtn');
    const skipBtn = document.getElementById('challengeSkipBtn');

    if (startBtn) startBtn.textContent = hasChallenge ? 'Restart Challenge' : 'Start Challenge';
    if (guessBtn) guessBtn.disabled = inactive;
    if (assistBtn) assistBtn.disabled = inactive || state.challenge.currentPuzzleAssistsRemaining <= 0;
    if (pauseBtn) pauseBtn.disabled = inactive || state.challenge.pauseUsed;
    if (skipBtn) skipBtn.disabled = inactive || state.challenge.skipUsed;
}

function resolveChallengeWordLength(dictionaryId) {
    return state.availableDictionaries.find(dict => dict.id === dictionaryId)?.wordLength || state.currentWordLength || 5;
}