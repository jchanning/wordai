/**
 * Client-side routing, view switching, and mobile panel/nav management.
 * Imports: state, ui, analytics (for refreshDictionaryScreen/switchDictTab).
 */
import { state } from './state.js';
import { showStatus } from './ui.js';
import { refreshDictionaryScreen, switchDictTab } from './analytics.js';

// ---- Router ----

export function initRouter() {
    window.addEventListener('hashchange', onRouteChange);
    onRouteChange();
}

export function onRouteChange() {
    const hash  = window.location.hash || '#/play';
    const route = hash.replace(/^#\//, '').trim();
    setView(route || 'play');
}

export function setView(view) {
    state.currentView = view;

    const viewIds = [
        'play', 'challenge', 'session', 'bot-demo', 'bot-performance',
        'dictionary', 'admin', 'help', 'about',
        'privacy', 'terms', 'cookies', 'terms-sale',
    ];

    viewIds.forEach(v => {
        const section = document.getElementById(`screen-${v}`);
        if (section) section.hidden = v !== view;
    });

    ensureGameViewHost(view);
    updateMobileBottomNavVisibility(view);
    handleMobilePanelMode();
    closeMobileNav();

    document.querySelectorAll('.nav-link[data-nav]').forEach(link => {
        const isActive = link.getAttribute('data-nav') === view;
        link.classList.toggle('active', isActive);
        if (isActive) link.setAttribute('aria-current', 'page');
        else          link.removeAttribute('aria-current');
    });

    // Ensure dictionary selectors are populated when switching to relevant views
    if (view === 'dictionary' && state.availableDictionaries.length > 0) {
        const selectorDict = document.getElementById('dictionarySelectorDict');
        if (selectorDict && selectorDict.options.length === 0) {
            populateDictionarySelector();
        }
        refreshDictionaryScreen();
        switchDictTab('frequency');
    } else if (view === 'play' && state.availableDictionaries.length > 0) {
        const selector = document.getElementById('dictionarySelector');
        if (selector && selector.options.length === 0) {
            populateDictionarySelector();
        }
    }

    if (view === 'help') loadHelpContent();
    if (view === 'admin') {
        // Defer to admin module via event to avoid circular import
        document.dispatchEvent(new CustomEvent('wordai:loadAdmin'));
    }
    if (view === 'challenge' && typeof window.onChallengeViewActivated === 'function') {
        window.onChallengeViewActivated();
    }
}

// ---- Help content ----

export async function loadHelpContent() {
    const helpContent = document.getElementById('helpContent');
    if (!helpContent || helpContent.dataset.loaded === 'true') return;

    helpContent.innerHTML = '<p style="color:#22c55e;">Fetching help content\u2026</p>';

    try {
        const response = await fetch('/help.html');
        if (!response.ok) throw new Error('Failed to load help content');
        const html = await response.text();

        const parser = new DOMParser();
        const doc = parser.parseFromString(html, 'text/html');
        const helpDiv = doc.querySelector('.help-content');

        helpContent.innerHTML = helpDiv ? helpDiv.innerHTML : doc.body.innerHTML;
        _setupHelpAnchorLinks(helpContent);
        helpContent.dataset.loaded = 'true';
    } catch (error) {
        console.error('Error loading help content:', error);
        helpContent.innerHTML = '<p style="color:#ef4444;">Error loading help content: ' + error.message + '<br>Please try the "Open in New Tab" button.</p>';
    }
}

function _setupHelpAnchorLinks(container) {
    container.querySelectorAll('a[href^="#"]').forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();
            const targetId = link.getAttribute('href').substring(1);
            const targetEl = container.querySelector('#' + targetId);
            if (targetEl) targetEl.scrollIntoView({ behavior: 'smooth', block: 'start' });
        });
    });
}

// ---- Layout helpers ----

export function ensureGameViewHost(view) {
    const gameView = document.getElementById('gameView');
    if (!gameView) return;
    const playHost = document.getElementById('playGameHost');
    const botHost  = document.getElementById('botDemoGameHost');
    if (!playHost || !botHost) return;
    const targetHost = view === 'bot-demo' ? botHost : playHost;
    if (gameView.parentElement !== targetHost) targetHost.appendChild(gameView);
}

export function updateMobileBottomNavVisibility(view) {
    const bottomNav = document.getElementById('mobileBottomNav');
    if (!bottomNav) return;
    if (view === 'play') {
        bottomNav.style.removeProperty('display');
    } else {
        bottomNav.style.setProperty('display', 'none', 'important');
    }
}

// ---- Dictionary selector population (called from setView) ----
// Forward-declared here; actual logic lives in game.js but is exposed globally.
// We call the global function if it exists to avoid an import cycle.
function populateDictionarySelector() {
    if (typeof window.populateDictionarySelector === 'function') {
        window.populateDictionarySelector();
    }
}

// ============================================================
// Mobile navigation (hamburger drawer)
// ============================================================

export function initMobileNavigation() {
    const hamburgerMenu = document.getElementById('hamburgerMenu');
    const appNav        = document.getElementById('appNav');
    if (!hamburgerMenu || !appNav) return;

    const overlay = document.createElement('div');
    overlay.className = 'mobile-nav-overlay';
    overlay.id = 'mobileNavOverlay';
    document.body.appendChild(overlay);

    hamburgerMenu.addEventListener('click', (e) => {
        e.stopPropagation();
        toggleMobileNav();
    });

    overlay.addEventListener('click', () => closeMobileNav());

    appNav.querySelectorAll('.nav-link').forEach(link => {
        link.addEventListener('click', function (e) {
            const nav = this.getAttribute('data-nav');
            closeMobileNav();
            if (nav) {
                e.preventDefault();
                window.location.hash = '#/' + nav;
                setView(nav);
            }
        });
    });

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && appNav.classList.contains('mobile-nav-open')) {
            closeMobileNav();
        }
    });

    initMobileViewSwitcher();
}

export function toggleMobileNav() {
    const hamburgerMenu = document.getElementById('hamburgerMenu');
    const appNav        = document.getElementById('appNav');
    const overlay       = document.getElementById('mobileNavOverlay');
    if (!hamburgerMenu || !appNav || !overlay) return;

    const isOpen = appNav.classList.toggle('mobile-nav-open');
    hamburgerMenu.classList.toggle('active');
    overlay.classList.toggle('active');
    hamburgerMenu.setAttribute('aria-expanded', isOpen);
    document.body.style.overflow = isOpen ? 'hidden' : '';
}

export function closeMobileNav() {
    const hamburgerMenu = document.getElementById('hamburgerMenu');
    const appNav        = document.getElementById('appNav');
    const overlay       = document.getElementById('mobileNavOverlay');
    if (!hamburgerMenu || !appNav || !overlay) return;

    appNav.classList.remove('mobile-nav-open');
    hamburgerMenu.classList.remove('active');
    overlay.classList.remove('active');
    hamburgerMenu.setAttribute('aria-expanded', 'false');
    document.body.style.overflow = '';
}

// ============================================================
// Mobile panel switcher (progressive disclosure on play screen)
// ============================================================

export function initMobileViewSwitcher() {
    document.querySelectorAll('.mobile-nav-btn[data-panel]').forEach(btn => {
        btn.addEventListener('click', function () {
            const panel = parseInt(this.getAttribute('data-panel'), 10);
            if (!isNaN(panel)) switchMobilePanel(panel);
        });
    });

    handleMobilePanelMode();
    window.addEventListener('resize', handleMobilePanelMode);
}

export function handleMobilePanelMode() {
    if (window.innerWidth >= 769) { showAllPanels(); return; }

    if (state.currentView !== 'play') {
        document.querySelectorAll('.history-panel, .game-container, .info-panel').forEach(el => {
            el.classList.remove('mobile-active');
        });
        document.querySelectorAll('[data-mobile-panel]').forEach(el => {
            el.classList.remove('mobile-panel-active');
        });
        return;
    }

    switchMobilePanel(state.currentMobilePanel);
}

/**
 * Switch to one of the 7 mobile panels.
 *   1 → game board
 *   2 → key statistics (info-panel sub-panel)
 *   3 → visual analysis (info-panel sub-panel)
 *   4 → letters by position (info-panel sub-panel)
 *   5 → session stats (history-panel sub-panel)
 *   6 → guess distribution (history-panel sub-panel)
 *   7 → recent games (history-panel sub-panel)
 */
export function switchMobilePanel(n) {
    if (window.innerWidth >= 769) return;

    state.currentMobilePanel = n;
    state.currentMobileView  = (n === 1) ? 'game' : (n <= 4) ? 'assistant' : 'session';

    document.querySelectorAll('.mobile-nav-btn[data-panel]').forEach(btn => {
        const isActive = parseInt(btn.getAttribute('data-panel'), 10) === n;
        btn.classList.toggle('active', isActive);
        btn.setAttribute('aria-current', isActive ? 'page' : 'false');
    });

    const historyPanel  = document.querySelector('.history-panel');
    const gameContainer = document.querySelector('.game-container');
    const infoPanel     = document.querySelector('.info-panel');
    historyPanel?.classList.remove('mobile-active');
    gameContainer?.classList.remove('mobile-active');
    infoPanel?.classList.remove('mobile-active');

    document.querySelectorAll('[data-mobile-panel]').forEach(el => {
        el.classList.remove('mobile-panel-active');
    });

    const parentMap = {
        1: gameContainer,
        2: infoPanel, 3: infoPanel, 4: infoPanel,
        5: historyPanel, 6: historyPanel,
    };
    const parent = parentMap[n];
    if (parent) { parent.classList.add('mobile-active'); parent.scrollTop = 0; }

    if (n >= 2) {
        const subPanel = document.querySelector(`[data-mobile-panel="${n}"]`);
        if (subPanel) { subPanel.classList.add('mobile-panel-active'); subPanel.scrollTop = 0; }
    }
}

/** Backward-compat alias — kept so any external/legacy call still works. */
export function switchMobileView(view) {
    const viewToPanel = { game: 1, assistant: 2, session: 5 };
    switchMobilePanel(viewToPanel[view] ?? 1);
}

export function showAllPanels() {
    if (window.innerWidth < 769) return;
    document.querySelectorAll('.history-panel, .game-container, .info-panel').forEach(panel => {
        panel.classList.remove('mobile-active');
    });
    document.querySelectorAll('[data-mobile-panel]').forEach(el => {
        el.classList.remove('mobile-panel-active');
    });
    document.querySelectorAll('.mobile-nav-btn').forEach(btn => {
        btn.classList.remove('active');
    });
}
