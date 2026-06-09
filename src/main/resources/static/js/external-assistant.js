/**
 * External assistant workflow for Wordle-style games played outside WordAI.
 * Isolates assistant session lifecycle from in-game Play session logic.
 */
import { state } from './state.js';
import {
    apiCreateAssistantSession,
    apiDeleteAssistantSession,
    apiSubmitAssistantFeedback,
    apiGetAssistantSuggestion,
    apiSetAssistantStrategy,
} from './api.js';
import { showStatus } from './ui.js';

const FEEDBACK_ORDER = ['R', 'A', 'G', 'X'];
const EMOJI_TO_CODE = {
    '🟩': 'G',
    '🟨': 'A',
    '⬛': 'R',
    '⬜': 'R',
};

export function initExternalAssistantUI() {
    _bindExternalAssistantInputs();
    _renderManualGuessInputs(state.manualAssistant.wordLength || 5);
    _renderFeedbackBuilder(state.manualAssistant.wordLength || 5);
    syncExternalAssistantUI();
}

export function syncExternalAssistantUI() {
    _setTextContent('manualAttemptCount', state.manualAssistant.attemptCount || 0);
    _setTextContent('manualRemainingWords', _formatDisplayValue(state.manualAssistant.remainingWords));
    _setTextContent('manualTotalWords', _formatDisplayValue(state.manualAssistant.totalWords));
    _setTextContent('manualEliminatedWords', _formatDisplayValue(state.manualAssistant.eliminatedWords));
    _setTextContent('manualReductionPercent', _formatPercent(state.manualAssistant.reductionPercent));
    _setTextContent('manualWordLength', _formatDisplayValue(state.manualAssistant.wordLength));
    _setTextContent('manualUniqueLetters', _formatDisplayValue(state.manualAssistant.uniqueLetters));
    _setTextContent('manualLetterCount', _formatDisplayValue(state.manualAssistant.letterCount));
    _setTextContent('manualStrategyLabel', _formatStrategyLabel(state.manualAssistant.strategy));
    _setTextContent('manualLastFeedback', state.manualAssistant.lastFeedback || '-');
    _setTextContent(
        'assistantPanelTitle',
        state.manualAssistant.lastSuggestion ? `Suggested Guess: ${state.manualAssistant.lastSuggestion}` : 'Suggested Guess: -'
    );

    _renderTurnHistory();
    _renderManualAnalytics();
}

async function ensureExternalAssistantSession() {
    if (state.manualAssistant.sessionId) return state.manualAssistant.sessionId;

    const dictionaryId = document.getElementById('assistantDictionarySelector')?.value
        || document.getElementById('dictionarySelector')?.value
        || null;
    const strategy = document.getElementById('externalStrategySelector')?.value
        || document.getElementById('strategySelector')?.value
        || 'ENTROPY';

    const response = await apiCreateAssistantSession({
        ...(dictionaryId ? { dictionaryId } : {}),
        strategy,
    });

    const data = await response.json();
    if (!response.ok) {
        throw new Error(data?.message || 'Failed to create external assistant session');
    }

    state.manualAssistant.sessionId = data.sessionId;
    state.manualAssistant.dictionaryId = data.dictionaryId || dictionaryId || null;
    state.manualAssistant.wordLength = data.wordLength || state.manualAssistant.wordLength || 5;
    state.manualAssistant.strategy = data.strategy || strategy;
    state.manualAssistant.totalWords = data.remainingWords ?? data.remainingWordsCount ?? state.manualAssistant.totalWords;
    state.manualAssistant.attemptCount = 0;
    state.manualAssistant.lastSuggestion = null;
    state.manualAssistant.remainingWords = data.remainingWords ?? data.remainingWordsCount ?? null;
    state.manualAssistant.eliminatedWords = 0;
    state.manualAssistant.reductionPercent = 0;
    state.manualAssistant.lastFeedback = null;
    state.manualAssistant.history = [];
    _applyDictionaryMetrics(data.dictionaryMetrics);
    _renderManualGuessInputs(state.manualAssistant.wordLength || 5);
    _renderFeedbackBuilder(state.manualAssistant.wordLength || 5);
    syncExternalAssistantUI();
    return data.sessionId;
}

export async function resetAssistantSession(showToast = true) {
    const sessionId = state.manualAssistant.sessionId;
    if (sessionId) {
        try {
            await apiDeleteAssistantSession(sessionId);
        } catch (error) {
            console.warn('Failed to delete external assistant session:', error);
        }
    }

    state.manualAssistant.sessionId = null;
    state.manualAssistant.attemptCount = 0;
    state.manualAssistant.remainingWords = null;
    state.manualAssistant.eliminatedWords = 0;
    state.manualAssistant.reductionPercent = 0;
    state.manualAssistant.lastSuggestion = null;
    state.manualAssistant.lastFeedback = null;
    state.manualAssistant.totalWords = null;
    state.manualAssistant.letterCount = null;
    state.manualAssistant.uniqueLetters = null;
    state.manualAssistant.latestColumnLengths = null;
    state.manualAssistant.latestOccurrenceData = null;
    state.manualAssistant.mostFrequentChars = null;
    state.manualAssistant.history = [];
    _setManualGuessWord('');
    _setManualFeedbackInput('');
    _renderManualGuessInputs(state.manualAssistant.wordLength || 5);
    _renderFeedbackBuilder(state.manualAssistant.wordLength || 5);
    syncExternalAssistantUI();

    if (showToast) {
        showStatus('Assistant ready for a new game', 'success');
    }
}

export async function onAssistantDictionaryChange() {
    await resetAssistantSession(false);
}

export async function changeAssistantStrategy() {
    const strategy = document.getElementById('externalStrategySelector')?.value;
    if (!strategy || !state.manualAssistant.sessionId) {
        return;
    }

    try {
        const response = await apiSetAssistantStrategy(state.manualAssistant.sessionId, strategy);
        if (!response.ok) {
            showStatus('Failed to update assistant strategy', 'error');
            return;
        }
        state.manualAssistant.strategy = strategy;
        syncExternalAssistantUI();
    } catch (error) {
        showStatus('Error updating strategy: ' + error.message, 'error');
    }
}

export async function applyManualFeedback(options = {}) {
    const { showSuccessStatus = true } = options;
    const guessedWord = _getManualGuessWord();
    const feedbackPattern = _getNormalizedFeedbackInput();

    if (!guessedWord || !feedbackPattern) {
        showStatus('Enter both guessed word and feedback pattern', 'error');
        return;
    }

    if (guessedWord.length !== feedbackPattern.length) {
        showStatus('Guess and feedback must have the same length', 'error');
        return;
    }

    const expectedWordLength = state.manualAssistant.wordLength || guessedWord.length;
    if (expectedWordLength && guessedWord.length !== expectedWordLength) {
        showStatus(`Guess must be ${expectedWordLength} letters for this assistant session`, 'error');
        return;
    }

    try {
        const sessionId = await ensureExternalAssistantSession();
        const response = await apiSubmitAssistantFeedback(sessionId, guessedWord, feedbackPattern);
        const data = await response.json();

        if (!response.ok) {
            throw new Error(data?.message || 'Failed to apply feedback');
        }

        const attemptNumber = data.attemptNumber ?? data.attemptCount ?? (state.manualAssistant.attemptCount + 1);
        const remainingWords = data.remainingWords ?? data.remainingWordsCount ?? state.manualAssistant.remainingWords;
        const normalizedFeedback = data.normalizedFeedback || feedbackPattern.toUpperCase();

        state.manualAssistant.attemptCount = attemptNumber;
        state.manualAssistant.remainingWords = remainingWords;
        state.manualAssistant.lastFeedback = normalizedFeedback;

        if (state.manualAssistant.totalWords === null || state.manualAssistant.totalWords === undefined) {
            state.manualAssistant.totalWords = remainingWords;
        }

        state.manualAssistant.eliminatedWords = Math.max(0, (state.manualAssistant.totalWords || 0) - (remainingWords || 0));
        state.manualAssistant.reductionPercent = state.manualAssistant.totalWords > 0
            ? ((state.manualAssistant.eliminatedWords / state.manualAssistant.totalWords) * 100)
            : 0;
        _applyDictionaryMetrics(data.dictionaryMetrics);

        state.manualAssistant.history.unshift({
            attempt: attemptNumber,
            guessedWord: guessedWord.toUpperCase(),
            feedback: normalizedFeedback,
            remainingWords,
        });

        const feedbackInput = document.getElementById('manualFeedbackInput');
        if (feedbackInput) feedbackInput.value = '';
        _renderManualGuessInputs(state.manualAssistant.wordLength || guessedWord.length || 5);
        _renderFeedbackBuilder(state.manualAssistant.wordLength || guessedWord.length || 5);

        syncExternalAssistantUI();
        if (showSuccessStatus) {
            showStatus('Feedback applied. Request recommendation for next guess.', 'success');
        }
    } catch (error) {
        showStatus('Error applying feedback: ' + error.message, 'error');
    }
}

export async function submitAssistantTurn() {
    const guessedWord = _getManualGuessWord();
    const feedbackPattern = _getNormalizedFeedbackInput();
    const hasTurnInput = guessedWord.length > 0 || feedbackPattern.length > 0;

    if (hasTurnInput) {
        await applyManualFeedback({ showSuccessStatus: false });
        if (_getNormalizedFeedbackInput()) {
            return;
        }
    }

    await requestAssistantSuggestion({ showSuccessStatus: true });
}

export async function requestAssistantSuggestion(options = {}) {
    const { showSuccessStatus = true } = options;
    try {
        const sessionId = await ensureExternalAssistantSession();
        const response = await apiGetAssistantSuggestion(sessionId);
        const data = await response.json();

        if (response.ok && data.suggestion) {
            const suggestion = data.suggestion.toUpperCase();
            state.manualAssistant.lastSuggestion = suggestion;
            state.manualAssistant.remainingWords = data.remainingWords ?? data.remainingWordsCount ?? state.manualAssistant.remainingWords;
            state.manualAssistant.strategy = data.strategy || state.manualAssistant.strategy;

            _setManualGuessWord(suggestion.toLowerCase());
            _renderManualGuessInputs(suggestion.length);
            _renderFeedbackBuilder(suggestion.length);

            if (state.manualAssistant.totalWords !== null && state.manualAssistant.totalWords !== undefined) {
                state.manualAssistant.eliminatedWords = Math.max(0, state.manualAssistant.totalWords - (state.manualAssistant.remainingWords || 0));
                state.manualAssistant.reductionPercent = state.manualAssistant.totalWords > 0
                    ? ((state.manualAssistant.eliminatedWords / state.manualAssistant.totalWords) * 100)
                    : 0;
            }

            syncExternalAssistantUI();
            if (showSuccessStatus) {
                showStatus('Recommendation ready: ' + suggestion, 'success');
            }
            return;
        }

        if (response.ok) {
            showStatus('No valid words remaining', 'error');
        } else {
            showStatus('Failed to get recommendation', 'error');
        }
    } catch (error) {
        showStatus('Error getting recommendation: ' + error.message, 'error');
    }
}

export function clearAssistantFeedbackPattern() {
    _setManualFeedbackInput('');
    _renderFeedbackBuilder(state.manualAssistant.wordLength || 5);
}

function _bindExternalAssistantInputs() {
    const feedbackInput = document.getElementById('manualFeedbackInput');

    if (feedbackInput) {
        feedbackInput.addEventListener('input', () => {
            const normalized = _normalizeFeedbackString(feedbackInput.value);
            if (normalized !== feedbackInput.value) {
                feedbackInput.value = normalized;
            }
            _renderFeedbackBuilder(undefined, normalized);
        });
    }
}

function _renderManualGuessInputs(length) {
    const guessHost = document.getElementById('manualGuessInputs');
    if (!guessHost) return;

    const expectedLength = length || state.manualAssistant.wordLength || 5;
    const existing = _getManualGuessWord().slice(0, expectedLength);

    guessHost.innerHTML = '';

    for (let i = 0; i < expectedLength; i++) {
        const input = document.createElement('input');
        input.type = 'text';
        input.maxLength = 1;
        input.autocomplete = 'off';
        input.spellcheck = false;
        input.className = 'manual-guess-letter';
        input.dataset.index = String(i);
        input.setAttribute('aria-label', `Guess letter ${i + 1}`);
        input.value = existing[i] || '';

        input.addEventListener('input', () => {
            const cleaned = (input.value || '').replace(/[^a-zA-Z]/g, '').toUpperCase();
            input.value = cleaned;
            if (cleaned && i < expectedLength - 1) {
                guessHost.children[i + 1]?.focus();
            }
        });

        input.addEventListener('keydown', (event) => {
            if (event.key === 'Backspace' && !input.value && i > 0) {
                const previous = guessHost.children[i - 1];
                previous?.focus();
            }
            if (event.key === 'ArrowLeft' && i > 0) {
                event.preventDefault();
                guessHost.children[i - 1]?.focus();
            }
            if (event.key === 'ArrowRight' && i < expectedLength - 1) {
                event.preventDefault();
                guessHost.children[i + 1]?.focus();
            }
        });

        input.addEventListener('paste', (event) => {
            event.preventDefault();
            const pasted = (event.clipboardData?.getData('text') || '')
                .replace(/[^a-zA-Z]/g, '')
                .toUpperCase()
                .slice(0, expectedLength);

            if (!pasted) return;

            for (let j = 0; j < expectedLength; j++) {
                const target = guessHost.children[j];
                if (target) {
                    target.value = pasted[j] || '';
                }
            }

            const finalIndex = Math.min(pasted.length, expectedLength - 1);
            guessHost.children[finalIndex]?.focus();
        });

        guessHost.appendChild(input);
    }
}

function _renderFeedbackBuilder(length, overridePattern) {
    const builder = document.getElementById('externalFeedbackBuilder');
    if (!builder) return;

    const expectedLength = length || state.manualAssistant.wordLength || 5;
    const currentPattern = overridePattern ?? _normalizeFeedbackString(document.getElementById('manualFeedbackInput')?.value || '');
    const paddedPattern = (currentPattern || '').padEnd(expectedLength, 'R').slice(0, expectedLength);

    builder.innerHTML = '';

    for (let i = 0; i < expectedLength; i++) {
        const code = paddedPattern[i] || 'R';
        const cell = document.createElement('button');
        cell.type = 'button';
        cell.className = `feedback-cell feedback-${code.toLowerCase()}`;
        cell.textContent = code;
        cell.dataset.index = String(i);
        cell.dataset.code = code;
        cell.setAttribute('aria-label', `Feedback position ${i + 1}: ${code}`);
        cell.addEventListener('click', () => {
            const currentCode = cell.dataset.code || 'R';
            const nextCode = FEEDBACK_ORDER[(FEEDBACK_ORDER.indexOf(currentCode) + 1) % FEEDBACK_ORDER.length];
            const feedbackInput = document.getElementById('manualFeedbackInput');
            const existing = _normalizeFeedbackString(feedbackInput?.value || '').padEnd(expectedLength, 'R').slice(0, expectedLength).split('');
            existing[i] = nextCode;
            _setManualFeedbackInput(existing.join(''));
            _renderFeedbackBuilder(expectedLength, existing.join(''));
        });
        builder.appendChild(cell);
    }

    if ((overridePattern ?? currentPattern) !== paddedPattern.trimEnd()) {
        _setManualFeedbackInput((overridePattern ?? currentPattern).slice(0, expectedLength));
    }
}

function _normalizeFeedbackString(rawValue) {
    if (!rawValue) return '';

    let value = rawValue;
    Object.entries(EMOJI_TO_CODE).forEach(([emoji, code]) => {
        value = value.split(emoji).join(code);
    });

    value = value.toUpperCase().replace(/\s+/g, '');
    return value.replace(/[^RAGX]/g, '');
}

function _getNormalizedFeedbackInput() {
    const feedbackInput = document.getElementById('manualFeedbackInput');
    const normalized = _normalizeFeedbackString(feedbackInput?.value || '');
    if (feedbackInput) {
        feedbackInput.value = normalized;
    }
    return normalized;
}

function _setManualFeedbackInput(value) {
    const feedbackInput = document.getElementById('manualFeedbackInput');
    if (feedbackInput) {
        feedbackInput.value = value;
    }
}

function _getManualGuessWord() {
    const inputs = Array.from(document.querySelectorAll('#manualGuessInputs .manual-guess-letter'));
    return inputs.map(input => (input.value || '').trim()).join('').toLowerCase();
}

function _setManualGuessWord(value) {
    const normalized = (value || '').replace(/[^a-zA-Z]/g, '').toUpperCase();
    const inputs = Array.from(document.querySelectorAll('#manualGuessInputs .manual-guess-letter'));

    if (inputs.length === 0) {
        return;
    }

    if (inputs.length !== normalized.length && normalized.length > 0) {
        _renderManualGuessInputs(normalized.length);
    }

    const updatedInputs = Array.from(document.querySelectorAll('#manualGuessInputs .manual-guess-letter'));
    updatedInputs.forEach((input, index) => {
        input.value = normalized[index] || '';
    });
}

function _formatStrategyLabel(strategy) {
    if (!strategy) return '-';
    const map = {
        RANDOM: 'Basic - Random',
        ENTROPY: 'Smart - Maximum Entropy',
        BELLMAN_FULL_DICTIONARY: 'Expert - Bellman Optimality',
    };
    return map[strategy] || strategy;
}

function _applyDictionaryMetrics(metrics) {
    if (!metrics) {
        return;
    }

    state.manualAssistant.letterCount = metrics.letterCount ?? null;
    state.manualAssistant.uniqueLetters = metrics.uniqueCharacters ?? null;
    state.manualAssistant.latestColumnLengths = null;
    if (Array.isArray(metrics.columnLengths)) {
        state.manualAssistant.latestColumnLengths = metrics.columnLengths;
    }
    state.manualAssistant.latestOccurrenceData = metrics.occurrenceCountByPosition || null;
    state.manualAssistant.mostFrequentChars = null;
    if (Array.isArray(metrics.mostFrequentCharByPosition)) {
        state.manualAssistant.mostFrequentChars = metrics.mostFrequentCharByPosition;
    }
}

function _renderManualAnalytics() {
    _renderManualColumnLengthsChart();
    _renderManualMostFrequentTable();
    _renderManualOccurrenceTable();
}

function _renderManualColumnLengthsChart() {
    const chartContainer = document.getElementById('manualColumnLengthsChart');
    if (!chartContainer) return;

    const columnLengths = state.manualAssistant.latestColumnLengths;
    chartContainer.innerHTML = '';

    if (!Array.isArray(columnLengths) || columnLengths.length === 0) {
        chartContainer.innerHTML = '<div class="external-assist-empty-state external-assist-empty-chart">No data yet</div>';
        return;
    }

    const maxHeight = Math.max(...columnLengths, 1);
    const chartHeight = 120;

    columnLengths.forEach((count, index) => {
        const barContainer = document.createElement('div');
        barContainer.style.cssText = 'flex:1;display:flex;flex-direction:column;align-items:center;gap:4px;';

        const barWrapper = document.createElement('div');
        barWrapper.style.cssText = `height:${chartHeight}px;display:flex;align-items:flex-end;justify-content:center;width:100%;`;

        const bar = document.createElement('div');
        const barHeight = Math.min(chartHeight, (count / maxHeight) * chartHeight);
        bar.style.cssText = `
            width:100%;
            height:${barHeight}px;
            background:linear-gradient(180deg,var(--accent-primary) 0%,#1e40af 100%);
            border-radius:4px 4px 0 0;
            transition:all 0.3s ease;
            box-shadow:0 2px 8px rgba(59,130,246,0.3);
        `;

        barWrapper.appendChild(bar);
        barContainer.appendChild(barWrapper);

        const countLabel = document.createElement('div');
        countLabel.className = 'count-label';
        countLabel.textContent = String(count);
        barContainer.appendChild(countLabel);

        const positionLabel = document.createElement('div');
        positionLabel.className = 'pos-label';
        positionLabel.textContent = `P${index + 1}`;
        barContainer.appendChild(positionLabel);

        chartContainer.appendChild(barContainer);
    });
}

function _renderManualMostFrequentTable() {
    const tableContainer = document.getElementById('manualMostFrequentTable');
    if (!tableContainer) return;

    const mostFrequentData = state.manualAssistant.mostFrequentChars;
    tableContainer.innerHTML = '';

    if (!Array.isArray(mostFrequentData) || mostFrequentData.length === 0) {
        tableContainer.innerHTML = '<div class="external-assist-empty-state">No data yet</div>';
        return;
    }

    const table = document.createElement('table');
    table.style.cssText = 'width:100%;border-collapse:collapse;font-size:0.9em;font-family:monospace;';

    const row = document.createElement('tr');
    mostFrequentData.forEach(letter => {
        const cell = document.createElement('td');
        cell.textContent = letter ? String(letter).toUpperCase() : '-';
        cell.style.cssText = 'padding:12px 8px;text-align:center;font-weight:700;font-size:1em;color:var(--correct-color);border:1px solid rgba(255,255,255,0.1);background:rgba(106,170,100,0.15);';
        row.appendChild(cell);
    });
    table.appendChild(row);

    const labelRow = document.createElement('tr');
    mostFrequentData.forEach((_, index) => {
        const labelCell = document.createElement('td');
        labelCell.textContent = `P${index + 1}`;
        labelCell.className = 'pos-label';
        labelCell.style.cssText = 'padding:6px 4px;text-align:center;border:1px solid rgba(255,255,255,0.1);';
        labelRow.appendChild(labelCell);
    });
    table.appendChild(labelRow);
    tableContainer.appendChild(table);
}

function _renderManualOccurrenceTable() {
    const tableContainer = document.getElementById('manualOccurrenceTable');
    if (!tableContainer) return;

    const occurrenceData = state.manualAssistant.latestOccurrenceData;
    tableContainer.innerHTML = '';

    if (!occurrenceData || Object.keys(occurrenceData).length === 0) {
        tableContainer.innerHTML = '<div class="external-assist-empty-state">No data yet</div>';
        return;
    }

    const firstKey = Object.keys(occurrenceData)[0];
    const numPositions = occurrenceData[firstKey]?.length || state.manualAssistant.wordLength || 5;

    const table = document.createElement('table');
    table.style.cssText = 'width:100%;border-collapse:collapse;font-size:0.85em;font-family:monospace;';

    const thead = document.createElement('thead');
    const headerRow = document.createElement('tr');

    const letterHeader = document.createElement('th');
    letterHeader.textContent = '';
    letterHeader.style.cssText = 'padding:6px 8px;text-align:center;border-bottom:2px solid var(--text-secondary);position:sticky;top:0;background:var(--bg-primary);z-index:1;font-weight:600;width:30px;';
    headerRow.appendChild(letterHeader);

    for (let i = 0; i < numPositions; i++) {
        const posHeader = document.createElement('th');
        posHeader.textContent = `P${i + 1}`;
        posHeader.className = 'pos-label';
        posHeader.style.cssText = 'padding:6px 6px;text-align:center;border-bottom:2px solid var(--text-secondary);position:sticky;top:0;background:var(--bg-primary);z-index:1;min-width:44px;';
        headerRow.appendChild(posHeader);
    }

    thead.appendChild(headerRow);
    table.appendChild(thead);

    const tbody = document.createElement('tbody');
    const allLetters = 'abcdefghijklmnopqrstuvwxyz'.split('');
    const positiveFeedbackMap = new Map();

    (state.manualAssistant.history || []).forEach(entry => {
        entry.feedback.split('').forEach((status, index) => {
            if (status === 'G') {
                positiveFeedbackMap.set(`${index}:${entry.guessedWord[index]}`, 'G');
            } else if (status === 'A') {
                positiveFeedbackMap.set(`${index}:${entry.guessedWord[index]}`, 'A');
            }
        });
    });

    allLetters.forEach((letter, idx) => {
        const row = document.createElement('tr');
        row.style.cssText = `border-bottom:1px solid rgba(255,255,255,0.1);${idx % 2 === 1 ? 'background:rgba(255,255,255,0.02);' : ''}`;

        const counts = occurrenceData[letter] || new Array(numPositions).fill(0);
        const isEliminated = counts.every(count => count === 0);

        const letterCell = document.createElement('td');
        letterCell.textContent = letter.toUpperCase();
        letterCell.style.cssText = 'padding:6px 4px;font-weight:bold;text-align:center;';

        if (isEliminated) {
            letterCell.classList.add('letter-status-absent');
            letterCell.style.opacity = '0.6';
            letterCell.style.textDecoration = 'line-through';
        } else if (Array.from(positiveFeedbackMap.keys()).some(key => key.endsWith(`:${letter.toUpperCase()}`))) {
            const hasCorrect = Array.from(positiveFeedbackMap.entries()).some(([key, status]) => key.endsWith(`:${letter.toUpperCase()}`) && status === 'G');
            letterCell.classList.add(hasCorrect ? 'letter-status-correct' : 'letter-status-present');
        } else {
            letterCell.classList.add('letter-status-unused');
        }
        row.appendChild(letterCell);

        const maxCount = Math.max(...counts);
        for (let i = 0; i < numPositions; i++) {
            const countCell = document.createElement('td');
            const count = counts[i] || 0;
            countCell.textContent = String(count);

            let bgColor = '';
            let textStyle = '';
            if (count === 0) {
                textStyle = 'color:var(--text-secondary);opacity:0.3;text-decoration:line-through;';
            } else if (maxCount > 0) {
                const intensity = count / maxCount;
                bgColor = `background:rgba(106,170,100,${intensity * 0.3});`;
                textStyle = 'color:var(--text-primary);font-weight:500;';
            }

            countCell.style.cssText = `padding:6px 6px;text-align:center;white-space:nowrap;${textStyle}${bgColor}`;
            row.appendChild(countCell);
        }

        tbody.appendChild(row);
    });

    table.appendChild(tbody);
    tableContainer.appendChild(table);
}

function _setTextContent(elementId, value) {
    const element = document.getElementById(elementId);
    if (element) {
        element.textContent = String(value);
    }
}

function _formatDisplayValue(value) {
    return value === null || value === undefined ? '-' : value;
}

function _formatPercent(value) {
    return value === null || value === undefined ? '-' : `${Number(value).toFixed(2)}%`;
}

function _renderTurnHistory() {
    const historyEl = document.getElementById('assistantTurnHistory');
    if (!historyEl) return;

    const history = state.manualAssistant.history || [];
    if (history.length === 0) {
        historyEl.innerHTML = '<p class="muted-center muted-small">No feedback submitted yet.</p>';
        return;
    }

    historyEl.innerHTML = history.map(item => {
        const remaining = item.remainingWords === null || item.remainingWords === undefined ? '-' : item.remainingWords;
        return `<div class="assistant-history-row">
            <span>#${item.attempt}</span>
            <span class="assistant-history-word">${item.guessedWord}</span>
            <span class="assistant-history-feedback">${_feedbackToChipMarkup(item.feedback)}</span>
            <span class="assistant-history-remaining">${remaining} left</span>
        </div>`;
    }).join('');
}

function _feedbackToChipMarkup(feedback) {
    if (!feedback) return '-';
    return feedback.split('').map(code => {
        const normalized = FEEDBACK_ORDER.includes(code) ? code : 'R';
        let stateClass = 'letter-absent';
        if (normalized === 'G') {
            stateClass = 'letter-correct';
        } else if (normalized === 'A') {
            stateClass = 'letter-present';
        }
        return `<span class="assistant-feedback-chip letter-box ${stateClass}">${normalized}</span>`;
    }).join('');
}
