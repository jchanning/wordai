/**
 * On-screen QWERTY keyboard — creation, letter-status tracking, display updates.
 * Imports: state, ui.
 */
import { state } from './state.js';
import { shouldSuppressNativeKeyboard } from './ui.js';

/**
 * Builds the on-screen keyboard DOM inside #gameKeyboard.
 * @param {Function} onGuess   Called when ENTER key is pressed.
 */
export function initializeKeyboard(onGuess) {
    const keyboardContainer = document.getElementById('gameKeyboard');
    if (!keyboardContainer) return;

    keyboardContainer.innerHTML = '';

    const rows = [
        ['Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P'],
        ['A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L'],
        ['Z', 'X', 'C', 'V', 'B', 'N', 'M'],
    ];

    rows.forEach((rowKeys, rowIndex) => {
        const rowDiv = document.createElement('div');
        rowDiv.className = 'keyboard-row';

        rowKeys.forEach(letter => {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'game-key key-unused';
            button.textContent = letter;
            button.dataset.letter = letter;
            button.setAttribute('aria-label', `Letter ${letter}`);

            button.addEventListener('click', function (e) {
                e.preventDefault();
                if (navigator.vibrate) navigator.vibrate(5);
                if (!this.disabled && !state.gameEnded) {
                    insertLetterFromKeyboard(letter);
                }
            });

            rowDiv.appendChild(button);
        });

        if (rowIndex === 2) {
            // ENTER key on the left — doubles as the Make Guess button
            const enterBtn = document.createElement('button');
            enterBtn.id = 'guessBtn';
            enterBtn.type = 'button';
            enterBtn.className = 'game-key wide-key enter-key';
            enterBtn.textContent = 'ENTER';
            enterBtn.setAttribute('aria-label', 'Enter / Make Guess');
            enterBtn.addEventListener('click', function (e) {
                e.preventDefault();
                if (!this.disabled && !state.gameEnded && onGuess) onGuess();
            });
            rowDiv.insertBefore(enterBtn, rowDiv.firstChild);

            // Backspace on the right
            const backspaceBtn = document.createElement('button');
            backspaceBtn.type = 'button';
            backspaceBtn.className = 'game-key wide-key';
            backspaceBtn.innerHTML = '&#9003;'; // ⌫
            backspaceBtn.setAttribute('aria-label', 'Backspace');
            backspaceBtn.addEventListener('click', function (e) {
                e.preventDefault();
                if (!state.gameEnded) deleteLastLetter();
            });
            rowDiv.appendChild(backspaceBtn);
        }

        keyboardContainer.appendChild(rowDiv);
    });

    // Reset display for the new game
    updateKeyboardDisplay({});
}

// ---- Letter insertion / deletion ----

export function insertLetterFromKeyboard(letter) {
    const inputs = document.querySelectorAll('.letter-input');
    const suppressKeyboard = shouldSuppressNativeKeyboard();
    let inserted = false;

    for (let i = 0; i < inputs.length; i++) {
        if (!inputs[i].value) {
            inputs[i].value = letter;
            inputs[i].classList.add('filled');
            if (!suppressKeyboard) inputs[i].focus();
            inserted = true;
            break;
        }
    }

    if (!inserted && inputs.length > 0 && !suppressKeyboard) {
        inputs[inputs.length - 1].focus();
    }
}

export function deleteLastLetter() {
    const inputs = document.querySelectorAll('.letter-input');
    let lastFilledIndex = -1;
    for (let i = 0; i < inputs.length; i++) {
        if (inputs[i].value) { lastFilledIndex = i; } else { break; }
    }

    if (lastFilledIndex >= 0) {
        const input = inputs[lastFilledIndex];
        input.value = '';
        input.classList.remove('filled');
        input.focus();
    } else if (inputs.length > 0) {
        inputs[0].focus();
    }
}

// ---- Letter status tracking ----

/**
 * Updates state.letterStatusMap with the best status for each letter
 * seen across all guesses (G > A > R).
 * @param {Array<{letter: string, status: string}>} results
 */
export function trackLetterStatus(results) {
    if (!results || !Array.isArray(results)) return;

    results.forEach(result => {
        const letter = (result.letter || '').toUpperCase();
        const status = result.status;
        const current = state.letterStatusMap[letter] || null;

        if (status === 'G') {
            state.letterStatusMap[letter] = 'G';
        } else if (status === 'A' && current !== 'G') {
            state.letterStatusMap[letter] = 'A';
        } else if ((status === 'R' || status === 'X') && !current) {
            state.letterStatusMap[letter] = 'R';
        }
    });

    updateKeyboardDisplay(state.letterStatusMap);
}

// ---- Keyboard display ----

/**
 * Re-colours all keyboard keys based on a letter→status map.
 * @param {Object} responseCounts  letter → 'G'|'A'|'R'|'X'
 */
export function updateKeyboardDisplay(responseCounts) {
    const keyboardContainer = document.getElementById('gameKeyboard');
    if (!keyboardContainer) return;

    keyboardContainer.querySelectorAll('.game-key').forEach(button => {
        const letter = (button.dataset.letter || '').toUpperCase();
        if (!letter) return;

        const status = responseCounts ? responseCounts[letter] : undefined;

        if (status === 'G') {
            button.className = 'game-key key-correct';
        } else if (status === 'A') {
            button.className = 'game-key key-present';
        } else if (status === 'R' || status === 'X') {
            button.className = 'game-key key-absent';
        } else {
            button.className = 'game-key key-unused';
        }
    });
}
