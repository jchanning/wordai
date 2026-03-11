/**
 * DOM helper functions used across the application.
 * Imports: state (for reading shared variables).
 * Does NOT import from other game modules — use callbacks for cross-module calls.
 */
import { state } from './state.js';

// ---- Native keyboard suppression ----

export function shouldSuppressNativeKeyboard() {
    return window.matchMedia('(pointer: coarse)').matches;
}

// ---- Letter input helpers ----

/**
 * Reads the current word typed into the letter-input grid.
 * @returns {string} Upper-cased word.
 */
export function getCurrentGuess() {
    const inputs = document.querySelectorAll('.letter-input');
    let word = '';
    inputs.forEach(input => { word += input.value.toUpperCase(); });
    return word;
}

/**
 * Clears all letter inputs and re-focuses the first one (unless on a touch device).
 */
export function clearLetterInputs() {
    const inputs = document.querySelectorAll('.letter-input');
    const suppressKeyboard = shouldSuppressNativeKeyboard();
    inputs.forEach(input => {
        input.value = '';
        input.classList.remove('filled');
        input.disabled = false;
    });
    if (!suppressKeyboard && inputs.length > 0) {
        inputs[0].focus();
    }
}

/**
 * Disables all letter inputs and on-screen keyboard buttons (called when game ends).
 */
export function disableLetterInputs() {
    document.querySelectorAll('.letter-input').forEach(input => { input.disabled = true; });
    document.querySelectorAll('.game-key').forEach(button => { button.disabled = true; });
}

/**
 * Attaches input / keydown / keypress event listeners to all .letter-input elements.
 * @param {Function} onEnter  Called when the user presses Enter (i.e. makeGuess).
 */
export function setupLetterInputs(onEnter) {
    const inputs = document.querySelectorAll('.letter-input');
    const suppressKeyboard = shouldSuppressNativeKeyboard();

    inputs.forEach((input, index) => {
        if (suppressKeyboard) {
            input.setAttribute('inputmode', 'none');
            input.setAttribute('readonly', 'readonly');
            input.setAttribute('autocomplete', 'off');
            input.setAttribute('autocapitalize', 'off');
            input.setAttribute('autocorrect', 'off');
        }

        input.addEventListener('input', function (e) {
            const value = e.target.value.toUpperCase();
            e.target.value = value;
            if (value) {
                e.target.classList.add('filled');
                if (index < inputs.length - 1) inputs[index + 1].focus();
            } else {
                e.target.classList.remove('filled');
            }
        });

        input.addEventListener('keydown', function (e) {
            if (suppressKeyboard) { e.preventDefault(); return; }
            if (e.key === 'Backspace' && !e.target.value && index > 0) {
                inputs[index - 1].focus();
                inputs[index - 1].value = '';
                inputs[index - 1].classList.remove('filled');
            }
            if (e.key === 'Enter' && !state.gameEnded && onEnter) {
                onEnter();
            }
        });

        input.addEventListener('keypress', function (e) {
            if (suppressKeyboard) { e.preventDefault(); return; }
            const char = String.fromCharCode(e.which);
            if (!/[a-zA-Z]/.test(char)) e.preventDefault();
        });

        if (suppressKeyboard) {
            input.addEventListener('focus', () => input.blur());
        }
    });
}

// ---- Status toast ----

export function isStatusInteracting(statusDiv) {
    if (!statusDiv) return false;
    if (statusDiv.matches(':hover')) return true;
    const active = document.activeElement;
    return !!(active && statusDiv.contains(active));
}

export function showStatus(message, type = 'info', options = {}) {
    const statusDiv = document.getElementById('status');
    if (!statusDiv) return;

    if (!statusDiv.dataset.escapeHandlerAttached) {
        statusDiv.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') { e.preventDefault(); hideStatus(); }
        });
        statusDiv.dataset.escapeHandlerAttached = 'true';
    }

    if (state.statusHideTimer) {
        clearTimeout(state.statusHideTimer);
        state.statusHideTimer = null;
    }

    const msgEl = document.getElementById('statusMessage');
    if (msgEl) {
        msgEl.textContent = message;
    } else {
        statusDiv.textContent = message;
    }

    if (options.anchorEl) {
        const rect = options.anchorEl.getBoundingClientRect();
        statusDiv.style.left = (rect.left + rect.width / 2) + 'px';
        statusDiv.style.top  = (rect.top  + rect.height / 2) + 'px';
    } else {
        statusDiv.style.left = '';
        statusDiv.style.top  = '';
    }

    const toastVariantClass = options.celebrate ? ' toast-celebrate' : (options.tilted ? ' toast-tilted' : '');
    statusDiv.className = `toast ${type}${toastVariantClass} toast-visible`;

    const isAlert = type === 'error' || type === 'warning';
    statusDiv.setAttribute('role', isAlert ? 'alert' : 'status');
    statusDiv.setAttribute('aria-live', isAlert ? 'assertive' : 'polite');

    const defaultAutoHideMs = (t) => {
        switch (t) {
            case 'success': return 3500;
            case 'info':    return 5000;
            case 'warning': return 8000;
            case 'error':   return 5000;
            default:        return 5000;
        }
    };

    const autoHideMs = Number.isFinite(options.autoHideMs) ? options.autoHideMs : defaultAutoHideMs(type);

    const dismissBtn = document.getElementById('statusDismiss');
    if (dismissBtn) {
        dismissBtn.style.display = autoHideMs === 0 ? 'inline-flex' : 'none';
    }

    if (autoHideMs > 0) {
        state.statusHideTimer = setTimeout(() => hideStatus(), autoHideMs);
    }

    // Dismiss on next user input (physical key or on-screen keyboard tap)
    if (state._statusInputDismiss) {
        document.removeEventListener('keydown', state._statusInputDismiss, true);
        document.removeEventListener('click',   state._statusInputDismiss, true);
        state._statusInputDismiss = null;
    }
    if (options.clearOnInput) {
        const dismiss = (e) => {
            if (e.type === 'click') {
                const t = e.target;
                if (!t.closest('.game-key') && !t.closest('.letter-input')) return;
            }
            document.removeEventListener('keydown', dismiss, true);
            document.removeEventListener('click',   dismiss, true);
            state._statusInputDismiss = null;
            hideStatus();
        };
        state._statusInputDismiss = dismiss;
        document.addEventListener('keydown', dismiss, true);
        document.addEventListener('click',   dismiss, true);
    }
}

export function hideStatus() {
    const statusDiv = document.getElementById('status');
    if (!statusDiv) return;
    if (state.statusHideTimer) {
        clearTimeout(state.statusHideTimer);
        state.statusHideTimer = null;
    }

    if (statusDiv.classList.contains('toast-celebrate')) {
        spawnExplosionParticles(statusDiv);
        statusDiv.classList.remove('toast-visible');
        statusDiv.classList.add('toast-exploding');
        setTimeout(() => {
            const msgEl = document.getElementById('statusMessage');
            if (msgEl) msgEl.textContent = '';
            statusDiv.className = 'toast';
            statusDiv.style.left = '';
            statusDiv.style.top  = '';
        }, 800);
    } else {
        statusDiv.classList.add('toast-hiding');
        statusDiv.classList.remove('toast-visible');
        setTimeout(() => {
            const msgEl = document.getElementById('statusMessage');
            if (msgEl) msgEl.textContent = '';
            statusDiv.className = 'toast';
            statusDiv.style.left = '';
            statusDiv.style.top  = '';
            statusDiv.classList.remove('toast-hiding');
        }, 220);
    }
}

function spawnExplosionParticles(anchor) {
    const rect = anchor.getBoundingClientRect();
    const cx = rect.left + rect.width / 2;
    const cy = rect.top + rect.height / 2;
    const colors = ['#86efac', '#22c55e', '#fbbf24', '#f59e0b', '#ffffff', '#34d399', '#a78bfa', '#f472b6'];
    const count = 30;

    for (let i = 0; i < count; i++) {
        const particle = document.createElement('div');
        const angle = (Math.PI * 2 * i) / count + (Math.random() - 0.5) * 0.5;
        const distance = 120 + Math.random() * 200;
        const tx = Math.cos(angle) * distance;
        const ty = Math.sin(angle) * distance;
        const size = 6 + Math.random() * 10;
        const color = colors[Math.floor(Math.random() * colors.length)];
        const rotation = Math.random() * 720 - 360;

        particle.style.cssText = `
            position: fixed;
            left: ${cx}px;
            top: ${cy}px;
            width: ${size}px;
            height: ${size}px;
            background: ${color};
            border-radius: ${Math.random() > 0.5 ? '50%' : '2px'};
            z-index: 9001;
            pointer-events: none;
        `;
        document.body.appendChild(particle);

        particle.animate([
            { transform: 'translate(-50%, -50%) rotate(0deg) scale(1)', opacity: 1 },
            { transform: `translate(calc(-50% + ${tx}px), calc(-50% + ${ty}px)) rotate(${rotation}deg) scale(0)`, opacity: 0 }
        ], {
            duration: 700 + Math.random() * 300,
            easing: 'cubic-bezier(0.25, 0.46, 0.45, 0.94)',
            fill: 'forwards'
        });

        setTimeout(() => particle.remove(), 1100);
    }
}

// ---- Help counter ----

export function updateHelpCounter() {
    const counterElement = document.getElementById('helpCounter');
    const button = document.getElementById('getSuggestionBtn');
    const remainingHelp = state.MAX_HELP_COUNT - state.helpUsedCount;

    if (counterElement) counterElement.textContent = `${remainingHelp}/${state.MAX_HELP_COUNT}`;

    if (!button) return;
    if (state.helpUsedCount >= state.MAX_HELP_COUNT) {
        button.disabled = true;
        button.style.backgroundColor = '#cccccc';
        button.style.cursor = 'not-allowed';
    } else {
        button.disabled = false;
        button.style.backgroundColor = '';
        button.style.cursor = 'pointer';
    }
}

// ---- Misc UI stubs ----

/* eslint-disable no-unused-vars */
export function updateAssistant(_status, _attempts = 0, _remainingWords = 0) {
    // Quick Stats section has been removed — kept for call-site compatibility.
}
