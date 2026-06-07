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
    _renderFeedbackBuilder(state.manualAssistant.wordLength || 5);
    syncExternalAssistantUI();
}

export function syncExternalAssistantUI() {
    const attemptCount = document.getElementById('manualAttemptCount');
    const remainingWords = document.getElementById('manualRemainingWords');
    const totalWords = document.getElementById('manualTotalWords');
    const eliminatedWords = document.getElementById('manualEliminatedWords');
    const reductionPercent = document.getElementById('manualReductionPercent');
    const wordLength = document.getElementById('manualWordLength');
    const strategyLabel = document.getElementById('manualStrategyLabel');
    const lastFeedback = document.getElementById('manualLastFeedback');
    const suggestionWord = document.getElementById('manualSuggestionWord');

    if (attemptCount) {
        attemptCount.textContent = String(state.manualAssistant.attemptCount || 0);
    }

    if (remainingWords) {
        const value = state.manualAssistant.remainingWords;
        remainingWords.textContent = value === null || value === undefined ? '-' : String(value);
    }

    if (totalWords) {
        const value = state.manualAssistant.totalWords;
        totalWords.textContent = value === null || value === undefined ? '-' : String(value);
    }

    if (eliminatedWords) {
        const value = state.manualAssistant.eliminatedWords;
        eliminatedWords.textContent = value === null || value === undefined ? '-' : String(value);
    }

    if (reductionPercent) {
        const value = state.manualAssistant.reductionPercent;
        reductionPercent.textContent = value === null || value === undefined ? '-' : `${Number(value).toFixed(2)}%`;
    }

    if (wordLength) {
        const value = state.manualAssistant.wordLength;
        wordLength.textContent = value === null || value === undefined ? '-' : String(value);
    }

    if (strategyLabel) {
        strategyLabel.textContent = _formatStrategyLabel(state.manualAssistant.strategy);
    }

    if (lastFeedback) {
        lastFeedback.textContent = state.manualAssistant.lastFeedback || '-';
    }

    if (suggestionWord) {
        suggestionWord.textContent = state.manualAssistant.lastSuggestion || '-';
    }

    _renderTurnHistory();
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
    state.manualAssistant.history = [];
    _setManualFeedbackInput('');
    _renderFeedbackBuilder(state.manualAssistant.wordLength || 5);
    syncExternalAssistantUI();

    if (showToast) {
        showStatus('External assistant session reset', 'success');
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

export async function applyManualFeedback() {
    const guessedWord = document.getElementById('manualGuessInput')?.value?.trim()?.toLowerCase() || '';
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

        state.manualAssistant.history.unshift({
            attempt: attemptNumber,
            guessedWord: guessedWord.toUpperCase(),
            feedback: normalizedFeedback,
            remainingWords,
        });

        const feedbackInput = document.getElementById('manualFeedbackInput');
        if (feedbackInput) feedbackInput.value = '';
        _renderFeedbackBuilder(state.manualAssistant.wordLength || guessedWord.length || 5);

        syncExternalAssistantUI();
        showStatus('Feedback applied. Request recommendation for next guess.', 'success');
    } catch (error) {
        showStatus('Error applying feedback: ' + error.message, 'error');
    }
}

export async function requestAssistantSuggestion() {
    try {
        const sessionId = await ensureExternalAssistantSession();
        const response = await apiGetAssistantSuggestion(sessionId);
        const data = await response.json();

        if (response.ok && data.suggestion) {
            const suggestion = data.suggestion.toUpperCase();
            state.manualAssistant.lastSuggestion = suggestion;
            state.manualAssistant.remainingWords = data.remainingWords ?? data.remainingWordsCount ?? state.manualAssistant.remainingWords;
            state.manualAssistant.strategy = data.strategy || state.manualAssistant.strategy;

            const guessInput = document.getElementById('manualGuessInput');
            if (guessInput) {
                guessInput.value = suggestion.toLowerCase();
            }
            _renderFeedbackBuilder(suggestion.length);

            if (state.manualAssistant.totalWords !== null && state.manualAssistant.totalWords !== undefined) {
                state.manualAssistant.eliminatedWords = Math.max(0, state.manualAssistant.totalWords - (state.manualAssistant.remainingWords || 0));
                state.manualAssistant.reductionPercent = state.manualAssistant.totalWords > 0
                    ? ((state.manualAssistant.eliminatedWords / state.manualAssistant.totalWords) * 100)
                    : 0;
            }

            syncExternalAssistantUI();
            showStatus('Recommendation ready: ' + suggestion, 'success');
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
    const guessInput = document.getElementById('manualGuessInput');
    const feedbackInput = document.getElementById('manualFeedbackInput');

    if (guessInput) {
        guessInput.addEventListener('input', () => {
            const guessLength = guessInput.value.trim().length;
            const baseLength = state.manualAssistant.wordLength || 5;
            const builderLength = guessLength > 0 ? guessLength : baseLength;
            _renderFeedbackBuilder(builderLength);
        });
    }

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
        cell.setAttribute('data-index', String(i));
        cell.setAttribute('data-code', code);
        cell.setAttribute('aria-label', `Feedback position ${i + 1}: ${code}`);
        cell.addEventListener('click', () => {
            const currentCode = cell.getAttribute('data-code') || 'R';
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

function _formatStrategyLabel(strategy) {
    if (!strategy) return '-';
    const map = {
        RANDOM: 'Basic - Random',
        ENTROPY: 'Smart - Maximum Entropy',
        BELLMAN_FULL_DICTIONARY: 'Expert - Bellman Optimality',
    };
    return map[strategy] || strategy;
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
        const stateClass = normalized === 'G'
            ? 'letter-correct'
            : normalized === 'A'
                ? 'letter-present'
                : 'letter-absent';
        return `<span class="assistant-feedback-chip letter-box ${stateClass}">${normalized}</span>`;
    }).join('');
}
