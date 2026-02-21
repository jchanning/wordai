const API_BASE = '/api/wordai';
let currentGameId = null;
let gameEnded = false;
let currentView = 'play';
let currentMobileView = 'game'; // Legacy alias — kept for external callers
let currentMobilePanel = 1;     // Active panel 1-7 (1 = game board)
let currentWordLength = 5; // Track the current game's word length
let currentDictionarySize = 2315; // Track the current dictionary's total word count
let helpUsedCount = 0; // Track how many times help has been used in this game
const MAX_HELP_COUNT = 3; // Maximum number of help requests allowed per game
let currentGameGuesses = []; // Track all guesses with their responses and metrics for current game
let currentUser = null; // Track logged-in user
let latestOccurrenceData = null; // Cache latest occurrence data to re-render with letter status

function shouldSuppressNativeKeyboard() {
    return window.matchMedia('(pointer: coarse)').matches;
}

// Initialize page
window.addEventListener('DOMContentLoaded', function() {
    checkAuthentication();
    initRouter();
    initMobileNavigation();

    // Create initial letter inputs with default word length
    adjustLetterInputGrid(5);
    
    // Load available algorithms and populate selectors
    loadAlgorithms();
    
    newGame();
    updateHistoryDisplay();
    updateStats();
    updateHelpCounter();
});

async function checkAuthentication() {
    try {
        const response = await fetch('/api/auth/user');
        if (response.ok) {
            currentUser = await response.json();
            displayUserInfo();
            updateUIForUserRole();
        } else {
            // Not authenticated, user can play anonymously
            currentUser = null;
            showGuestOptions();
            updateUIForUserRole();
        }
    } catch (error) {
        // Not authenticated, user can play anonymously
        console.log('Playing as guest');
        currentUser = null;
        showGuestOptions();
        updateUIForUserRole();
    }
}

async function loadAlgorithms() {
    try {
        console.log('Loading algorithms from API...');
        const response = await fetch(`${API_BASE}/algorithms`);
        if (!response.ok) {
            console.error('Failed to load algorithms, status:', response.status);
            return;
        }
        
        const algorithms = await response.json();
        console.log('Loaded algorithms:', algorithms);
        
        // Update all strategy selectors
        const selectors = [
            'strategySelector',
            'autoplayStrategy',
            'analysisStrategy'
        ];
        
        selectors.forEach(selectorId => {
            const selector = document.getElementById(selectorId);
            if (selector) {
                const currentValue = selector.value; // Preserve current selection
                
                // Clear existing options
                selector.innerHTML = '';
                
                // Add algorithm options (only include enabled algorithms)
                algorithms.forEach(algo => {
                    // Skip disabled algorithms - remove from UI entirely
                    if (algo.enabled === 'false') {
                        console.log('Skipping disabled algorithm:', algo.id);
                        return;
                    }
                    
                    console.log('Adding algorithm:', algo.id, 'to', selectorId);
                    const option = document.createElement('option');
                    option.value = algo.id;
                    option.textContent = algo.name;
                    option.title = algo.description;
                    
                    selector.appendChild(option);
                });
                
                // Restore selection if still valid and enabled
                if (currentValue) {
                    const option = Array.from(selector.options).find(opt => opt.value === currentValue);
                    if (option && !option.disabled) {
                        selector.value = currentValue;
                    } else {
                        // Select first enabled option
                        const firstEnabled = Array.from(selector.options).find(opt => !opt.disabled);
                        if (firstEnabled) {
                            selector.value = firstEnabled.value;
                        }
                    }
                }
            }
        });
        
    } catch (error) {
        console.error('Error loading algorithms:', error);
    }
}

function updateUIForUserRole() {
    // Hide/show features based on user role
    const premiumFeatures = document.querySelectorAll('[data-role="premium"]');
    const adminFeatures = document.querySelectorAll('[data-role="admin"]');
    const userFeatures = document.querySelectorAll('[data-role="user"]');
    
    // Premium features (analytics, export, etc.)
    premiumFeatures.forEach(element => {
        element.style.display = (currentUser && (isUserPremium() || isUserAdmin())) ? 'block' : 'none';
    });
    
    // Admin features
    adminFeatures.forEach(element => {
        element.style.display = (currentUser && isUserAdmin()) ? 'block' : 'none';
    });
    
    // Registered user features (stats, history)
    userFeatures.forEach(element => {
        element.style.display = currentUser ? 'block' : 'none';
    });
    
    // Add role indicators to UI
    updateRoleIndicators();
}

function isUserAdmin() {
    return currentUser && currentUser.roles && currentUser.roles.includes('ROLE_ADMIN');
}

function isUserPremium() {
    return currentUser && currentUser.roles && currentUser.roles.includes('ROLE_PREMIUM');
}

function isUserRegistered() {
    return currentUser && currentUser.roles && currentUser.roles.includes('ROLE_USER');
}

function getUserPrimaryRole() {
    if (!currentUser) return 'Guest';
    return currentUser.primaryRole || 'Guest';
}

function updateRoleIndicators() {
    const roleIndicator = document.getElementById('roleIndicator');
    const userRole = getUserPrimaryRole();
    
    if (roleIndicator) {
        roleIndicator.textContent = userRole;
        roleIndicator.className = 'role-badge role-' + userRole.toLowerCase().replace(' ', '-');
    }
    
    // Update navigation with admin link
    updateNavigation();
}

function updateNavigation() {
    let adminLink = document.getElementById('adminLink');
    
    if (isUserAdmin()) {
        if (!adminLink) {
            adminLink = document.createElement('a');
            adminLink.id = 'adminLink';
            adminLink.href = '/admin.html';
            adminLink.textContent = 'Admin Panel';
            adminLink.style.marginLeft = '15px';
            adminLink.style.color = '#dc3545';
            adminLink.style.textDecoration = 'none';
            
            const userInfo = document.getElementById('userInfo');
            if (userInfo) {
                userInfo.appendChild(adminLink);
            }
        }
    } else if (adminLink) {
        adminLink.remove();
    }
}

function displayUserInfo() {
    if (currentUser) {
        const userNameElement = document.getElementById('userName');
        const userInfoElement = document.getElementById('userInfo');
        const signInLink = document.getElementById('signInLink');
        
        if (userNameElement && userInfoElement) {
            userNameElement.textContent = currentUser.fullName || currentUser.username || currentUser.email;
            userInfoElement.style.display = 'flex';
        }
        
        if (signInLink) {
            signInLink.style.display = 'none';
        }
        
        // Show or hide Admin nav link based on roles
        const adminNavLink = document.querySelector('[data-nav="admin"]');
        if (adminNavLink) {
            const isAdmin = currentUser.roles && currentUser.roles.includes('ROLE_ADMIN');
            adminNavLink.style.display = isAdmin ? '' : 'none';
        }
    }
}

function showGuestOptions() {
    const userInfoElement = document.getElementById('userInfo');
    const signInLink = document.getElementById('signInLink');
    
    if (userInfoElement) {
        userInfoElement.style.display = 'none';
    }
    
    // Hide Admin nav link for guests
    const adminNavLink = document.querySelector('[data-nav="admin"]');
    if (adminNavLink) {
        adminNavLink.style.display = 'none';
    }
    
    if (signInLink) {
        signInLink.style.display = 'inline-flex';
    }
}

function logout() {
    window.location.href = '/api/auth/logout';
}

function initRouter() {
    window.addEventListener('hashchange', onRouteChange);
    onRouteChange();
}

function onRouteChange() {
    const hash = window.location.hash || '#/play';
    const route = hash.replace(/^#\//, '').trim();
    const view = route || 'play';
    setView(view);
}

function setView(view) {
    currentView = view;
    const viewIds = ['play', 'session', 'bot-demo', 'bot-performance', 'dictionary', 'admin', 'help'];
    viewIds.forEach(v => {
        const section = document.getElementById(`screen-${v}`);
        if (section) {
            section.hidden = v !== view;
        }
    });

    ensureGameViewHost(view);
    updateMobileBottomNavVisibility(view);
    handleMobilePanelMode();
    closeMobileNav();

    document.querySelectorAll('.nav-link[data-nav]').forEach(link => {
        const isActive = link.getAttribute('data-nav') === view;
        link.classList.toggle('active', isActive);
        if (isActive) {
            link.setAttribute('aria-current', 'page');
        } else {
            link.removeAttribute('aria-current');
        }
    });

    // Ensure dictionary selectors are populated when switching to relevant views
    if (view === 'dictionary' && availableDictionaries.length > 0) {
        const selectorDict = document.getElementById('dictionarySelectorDict');
        if (selectorDict && selectorDict.options.length === 0) {
            populateDictionarySelector();
        }
        refreshDictionaryScreen();
    } else if (view === 'play' && availableDictionaries.length > 0) {
        const selector = document.getElementById('dictionarySelector');
        if (selector && selector.options.length === 0) {
            populateDictionarySelector();
        }
    }
    
    if (view === 'help') {
        loadHelpContent();
    }
    
    if (view === 'admin') {
        loadAdminScreen();
    }
}

function updateMobileBottomNavVisibility(view) {
    const bottomNav = document.getElementById('mobileBottomNav');
    if (!bottomNav) {
        return;
    }

    if (view === 'play') {
        bottomNav.style.removeProperty('display');
    } else {
        bottomNav.style.setProperty('display', 'none', 'important');
    }
}

function ensureGameViewHost(view) {
    const gameView = document.getElementById('gameView');
    if (!gameView) {
        return;
    }

    const playHost = document.getElementById('playGameHost');
    const botHost = document.getElementById('botDemoGameHost');
    if (!playHost || !botHost) {
        return;
    }

    const targetHost = view === 'bot-demo' ? botHost : playHost;
    if (gameView.parentElement !== targetHost) {
        targetHost.appendChild(gameView);
    }
}

async function loadHelpContent() {
    const helpContent = document.getElementById('helpContent');
    if (!helpContent) {
        return;
    }
    
    // Check if content is already loaded
    if (helpContent.dataset.loaded === 'true') {
        return;
    }
    
    helpContent.innerHTML = '<p style="color: #22c55e;">Fetching help content...</p>';
    
    try {
        const response = await fetch('/help.html');
        if (!response.ok) {
            throw new Error('Failed to load help content');
        }
        const html = await response.text();
        
        // Extract the content from inside the help-content div
        const parser = new DOMParser();
        const doc = parser.parseFromString(html, 'text/html');
        const helpDiv = doc.querySelector('.help-content');
        
        if (helpDiv) {
            // Get just the inner HTML of the help-content div
            helpContent.innerHTML = helpDiv.innerHTML;
        } else {
            // Fallback to body content if help-content div not found
            helpContent.innerHTML = doc.body.innerHTML;
        }
        
        // Setup anchor link handlers to scroll within the help content
        setupHelpAnchorLinks(helpContent);
        
        helpContent.dataset.loaded = 'true';
    } catch (error) {
        console.error('Error loading help content:', error);
        helpContent.innerHTML = '<p style="color: #ef4444;">Error loading help content: ' + error.message + '<br>Please try the "Open in New Tab" button.</p>';
    }
}

function setupHelpAnchorLinks(container) {
    // Find all anchor links that start with #
    const anchorLinks = container.querySelectorAll('a[href^="#"]');
    
    anchorLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();
            
            const targetId = link.getAttribute('href').substring(1);
            const targetElement = container.querySelector('#' + targetId);
            
            if (targetElement) {
                // Scroll the target element into view smoothly
                targetElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }
        });
    });
}

function setupLetterInputs() {
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
            if (suppressKeyboard) {
                e.preventDefault();
                return;
            }
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
            if (suppressKeyboard) {
                e.preventDefault();
                return;
            }
            const char = String.fromCharCode(e.which);
            if (!/[a-zA-Z]/.test(char)) {
                e.preventDefault();
            }
        });

        if (suppressKeyboard) {
            input.addEventListener('focus', () => input.blur());
        }
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

function disableLetterInputs() {
    const inputs = document.querySelectorAll('.letter-input');
    inputs.forEach(input => {
        input.disabled = true;
    });
    
    // Also disable keyboard buttons when game ends
    const keyboardButtons = document.querySelectorAll('.game-key');
    keyboardButtons.forEach(button => {
        button.disabled = true;
    });
}

let statusHideTimer = null;

function isStatusInteracting(statusDiv) {
    if (!statusDiv) return false;
    if (statusDiv.matches(':hover')) return true;
    const active = document.activeElement;
    return !!(active && statusDiv.contains(active));
}

function showStatus(message, type = 'info', options = {}) {
    const statusDiv = document.getElementById('status');
    if (!statusDiv) {
        return;
    }

    // Add Escape-to-dismiss once (delegated from focusable children like the dismiss button)
    if (!statusDiv.dataset.escapeHandlerAttached) {
        statusDiv.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                e.preventDefault();
                hideStatus();
            }
        });
        statusDiv.dataset.escapeHandlerAttached = 'true';
    }

    if (statusHideTimer) {
        clearTimeout(statusHideTimer);
        statusHideTimer = null;
    }

    const msgEl = document.getElementById('statusMessage');
    if (msgEl) {
        msgEl.textContent = message;
    } else {
        statusDiv.textContent = message;
    }

    statusDiv.className = `status ${type}`;

    // Accessibility: errors/warnings should be more assertive
    const isAlert = type === 'error' || type === 'warning';
    statusDiv.setAttribute('role', isAlert ? 'alert' : 'status');
    statusDiv.setAttribute('aria-live', isAlert ? 'assertive' : 'polite');

    statusDiv.style.display = 'flex';

    const defaultAutoHideMs = (t) => {
        switch (t) {
            case 'success':
                return 3500;
            case 'info':
                return 5000;
            case 'warning':
                return 8000;
            case 'error':
                return 0;
            default:
                return 5000;
        }
    };

    const autoHideMs = Number.isFinite(options.autoHideMs) ? options.autoHideMs : defaultAutoHideMs(type);
    if (autoHideMs > 0) {
        statusHideTimer = setTimeout(() => hideStatus(), autoHideMs);
    }
}

function hideStatus() {
    const statusDiv = document.getElementById('status');
    if (!statusDiv) {
        return;
    }
    if (statusHideTimer) {
        clearTimeout(statusHideTimer);
        statusHideTimer = null;
    }
    const msgEl = document.getElementById('statusMessage');
    if (msgEl) {
        msgEl.textContent = '';
    }
    statusDiv.className = 'status';
    statusDiv.style.display = 'flex';
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
        
        // Reset keyboard letter status tracking for new game
        window.letterStatusMap = {};
        latestOccurrenceData = null;
        
        // Create letter inputs based on word length
        adjustLetterInputGrid(data.wordLength);
        
        document.getElementById('attempts').textContent = '0';
        document.getElementById('maxAttempts').textContent = data.maxAttempts;
        document.getElementById('guessHistory').innerHTML = '';
        const guessBtnNew = document.getElementById('guessBtn');
        if (guessBtnNew) guessBtnNew.disabled = false;
        
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
        
        // Reset current game guesses
        currentGameGuesses = [];
        
        // Set the strategy for the new game based on dropdown selection
        await changeStrategy();

        // On mobile, always navigate to the game panel when starting a new game
        if (window.innerWidth <= 768) switchMobileView('game');
    } catch (error) {
        console.error('Failed to create new game:', error);
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

    // Validate word length against actual number of inputs (more reliable than currentWordLength)
    const expectedLength = document.querySelectorAll('.letter-input').length;
    if (word.length !== expectedLength) {
        showStatus(`Word must be ${expectedLength} letters long!`, 'error');
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
            const guessBtnWon = document.getElementById('guessBtn');
            if (guessBtnWon) guessBtnWon.disabled = true;
            showStatus('🎉 Congratulations! You won!', 'success');
            updateAssistant('won', data.attemptNumber);
            // Save game result
            saveGameResult(word, data.attemptNumber, true);
        } else if (data.gameOver) {
            gameEnded = true;
            disableLetterInputs();
            const guessBtnOver = document.getElementById('guessBtn');
            if (guessBtnOver) guessBtnOver.disabled = true;
            // Fetch game status to get target word and update status
            fetchTargetWordAndSave(data.attemptNumber);
        } else {
            updateAssistant('playing', data.attemptNumber, data.remainingWordsCount);
        }

    } catch (error) {
        console.error('Error making guess:', error);
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
    
    // Auto-scroll to the bottom to ensure the latest guess is visible
    historyDiv.scrollTop = historyDiv.scrollHeight;
    
    // Update on-screen keyboard with letter feedback
    trackLetterStatus(results);

    // Re-render occurrence table with current letter statuses so colors stay in sync
    if (latestOccurrenceData) {
        updateOccurrenceTable(latestOccurrenceData);
    }
}

async function checkHealth() {
    try {
        const response = await fetch(`${API_BASE}/health`);
        const data = await response.json();
        
        if (response.ok) {
            console.log('Server is running. Active sessions:', data.activeSessions);
        } else {
            console.error('Server health check failed');
        }
    } catch (error) {
        console.error('Cannot connect to server:', error);
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
        latestOccurrenceData = dictionaryMetrics.occurrenceCountByPosition;
        updateOccurrenceTable(latestOccurrenceData);
    } else {
        latestOccurrenceData = null;
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
            latestOccurrenceData = dictionaryMetrics.occurrenceCountByPosition;
            updateOccurrenceTable(latestOccurrenceData);
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
    const chartHeight = 120; // Height available for bars within 180px container (padding leaves room for labels)
    
    columnLengths.forEach((count, index) => {
        const barContainer = document.createElement('div');
        barContainer.style.cssText = 'flex: 1; display: flex; flex-direction: column; align-items: center; gap: 4px;';
        
        // Create bar wrapper for positioning
        const barWrapper = document.createElement('div');
        barWrapper.style.cssText = `height: ${chartHeight}px; display: flex; align-items: flex-end; justify-content: center; width: 100%;`;
        
        // Calculate bar height as percentage of max (26)
        const heightPercent = (count / maxHeight) * 100;
        const barHeight = Math.min(chartHeight, (heightPercent / 100) * chartHeight);
        
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
        countLabel.className = 'count-label';
        countLabel.textContent = count;
        barContainer.appendChild(countLabel);
        
        // Add position label below count
        const positionLabel = document.createElement('div');
        positionLabel.className = 'pos-label';
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
    
    // Get number of positions from first entry
    const firstKey = Object.keys(occurrenceData)[0];
    const numPositions = occurrenceData[firstKey] ? occurrenceData[firstKey].length : 5;

    // Create table
    // width: 100% fills the right column; the min-width on each header cell
    // (44px per position) ensures columns are readable for any word length.
    const table = document.createElement('table');
    table.style.cssText = 'width: 100%; border-collapse: collapse; font-size: 0.85em; font-family: monospace;';
    
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
        posHeader.className = 'pos-label';
        posHeader.style.cssText = 'padding: 6px 6px; text-align: center; border-bottom: 2px solid var(--text-secondary); position: sticky; top: 0; background: var(--bg-primary); z-index: 1; min-width: 44px;';
        headerRow.appendChild(posHeader);
    }
    
    thead.appendChild(headerRow);
    table.appendChild(thead);
    
    // Create body - iterate through ALL letters a-z (not just what's in occurrenceData)
    const tbody = document.createElement('tbody');
    
    // All 26 letters
    const allLetters = 'abcdefghijklmnopqrstuvwxyz'.split('');
    const letterStatusMap = (window.letterStatusMap) ? window.letterStatusMap : {};
    
    allLetters.forEach((letter, idx) => {
        const row = document.createElement('tr');
        row.style.cssText = `border-bottom: 1px solid rgba(255,255,255,0.1); ${idx % 2 === 1 ? 'background: rgba(255,255,255,0.02);' : ''}`;
        
        // Get counts for this letter (if not in data, all positions are 0 = eliminated)
        const counts = occurrenceData[letter] || Array(numPositions).fill(0);
        const isEliminated = counts.every(count => count === 0);
        
        // Letter cell - centered with reduced padding and status color
        const letterCell = document.createElement('td');
        letterCell.textContent = letter.toUpperCase();
        letterCell.style.cssText = 'padding: 6px 4px; font-weight: bold; text-align: center;';

        const status = letterStatusMap[letter.toUpperCase()];
        if (status === 'G') {
            letterCell.classList.add('letter-status-correct');
        } else if (status === 'A') {
            letterCell.classList.add('letter-status-present');
        } else if (status === 'R' || status === 'X') {
            letterCell.classList.add('letter-status-absent');
        } else {
            letterCell.classList.add('letter-status-unused');
        }

        if (isEliminated) {
            letterCell.style.opacity = '0.6';
            letterCell.style.textDecoration = 'line-through';
        }
        row.appendChild(letterCell);
        
        // Position count cells
        const maxCount = Math.max(...counts);
        
        for (let i = 0; i < numPositions; i++) {
            const countCell = document.createElement('td');
            const count = counts[i] || 0;
            countCell.textContent = count;
            
            // Highlight cells with higher counts, or grey out if eliminated at this position
            let bgColor = '';
            let textStyle = '';
            
            if (count === 0) {
                // Greyed out for eliminated positions
                textStyle = 'color: var(--text-secondary); opacity: 0.3; text-decoration: line-through;';
            } else if (maxCount > 0) {
                const intensity = count / maxCount;
                bgColor = `background: rgba(106, 170, 100, ${intensity * 0.3});`;
                textStyle = 'color: var(--text-primary); font-weight: 500;';
            }
            
            countCell.style.cssText = `padding: 6px 6px; text-align: center; white-space: nowrap; ${textStyle} ${bgColor}`;
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
        cell.style.cssText = `padding: 12px 8px; text-align: center; font-weight: 700; font-size: 1em; color: var(--correct-color); border: 1px solid rgba(255,255,255,0.1); background: rgba(106, 170, 100, 0.15);`;
        row.appendChild(cell);
    });
    
    table.appendChild(row);
    
    // Create position labels row
    const labelRow = document.createElement('tr');
    
    mostFrequentData.forEach((letter, index) => {
        const labelCell = document.createElement('td');
        labelCell.textContent = `P${index + 1}`;
        labelCell.className = 'pos-label';
        labelCell.style.cssText = 'padding: 6px 4px; text-align: center; border: 1px solid rgba(255,255,255,0.1);';
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
    // Save to history (all games for export, only display last 5)
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
    
    // Store ALL games (no limit) - needed for full CSV export
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

function exportSessionGamesToCSV() {
    const history = getGameHistory();
    
    if (history.length === 0) {
        console.log('No games to export');
        return;
    }
    
    // CSV header
    let csv = 'Game#,Target Word,Word Length,Dictionary Size,Result,Attempts,Timestamp,Guesses\n';
    
    // Add each game (reverse to show oldest first in export)
    const orderedHistory = [...history].reverse();
    orderedHistory.forEach((game, index) => {
        const gameNum = index + 1;
        const targetWord = game.targetWord.toUpperCase();
        const wordLength = game.wordLength || targetWord.length;
        const dictionarySize = game.dictionarySize || 'N/A';
        const result = game.won ? 'WON' : 'LOST';
        const attempts = game.attempts;
        const timestamp = game.timestamp ? new Date(game.timestamp).toLocaleString() : 'N/A';
        
        // Format guesses as comma-separated uppercase words
        let guessesStr = '';
        if (game.guesses && game.guesses.length > 0) {
            guessesStr = game.guesses.map(g => g.guess ? g.guess.toUpperCase() : '').join('; ');
        }
        
        // Escape fields that might contain commas
        const escapedTimestamp = `"${timestamp}"`;
        const escapedGuesses = `"${guessesStr}"`;
        
        csv += `${gameNum},${targetWord},${wordLength},${dictionarySize},${result},${attempts},${escapedTimestamp},${escapedGuesses}\n`;
    });
    
    // Create blob and download
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    
    const now = new Date();
    const dateStr = now.toISOString().slice(0, 10);
    const timeStr = now.toTimeString().slice(0, 8).replace(/:/g, '-');
    const filename = `wordai-session-${dateStr}_${timeStr}.csv`;
    
    link.setAttribute('href', url);
    link.setAttribute('download', filename);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}

function updateAttemptsDistribution() {
    const stats = getGameStats();
    const container = document.getElementById('attemptsDistribution');
    
    if (stats.total === 0) {
        container.innerHTML = '<p style="text-align: center; color: var(--text-secondary); font-size: 0.85em;">No games played yet</p>';
        return;
    }
    
    // Count attempts 1-6 and failures
    const distribution = { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0, 6: 0, fail: stats.lost };
    
    stats.wonGames.forEach(attempts => {
        if (attempts >= 1 && attempts <= 6) {
            distribution[attempts]++;
        }
    });
    
    // Find max count for scaling
    const maxCount = Math.max(...Object.values(distribution));
    
    // Build chart HTML
    let chartHTML = '<div style="display: flex; flex-direction: column; gap: 6px;">';
    
    for (let i = 1; i <= 6; i++) {
        const count = distribution[i];
        const percentage = maxCount > 0 ? (count / maxCount) * 100 : 0;
        const barColor = 'var(--correct)';
        
        chartHTML += `
            <div style="display: flex; align-items: center; gap: 8px;">
                <div style="min-width: 12px; text-align: right; font-size: 0.9em; font-weight: 600; color: var(--text-primary);">${i}</div>
                <div style="flex: 1; height: 24px; background: rgba(106, 170, 100, 0.2); border-radius: 4px; overflow: hidden; position: relative;">
                    <div style="height: 100%; width: ${percentage}%; background: ${barColor}; transition: width 0.3s ease; display: flex; align-items: center; justify-content: flex-end; padding-right: 6px;">
                        ${count > 0 ? `<span style="font-size: 0.85em; font-weight: 600; color: white;">${count}</span>` : ''}
                    </div>
                    ${count === 0 ? `<div style="position: absolute; left: 6px; top: 50%; transform: translateY(-50%); font-size: 0.85em; color: var(--text-secondary);">${count}</div>` : ''}
                </div>
            </div>
        `;
    }
    
    // Add failures bar (in red)
    const failCount = distribution.fail;
    const failPercentage = maxCount > 0 ? (failCount / maxCount) * 100 : 0;
    const failBarColor = 'var(--error)';
    
    chartHTML += `
        <div style="display: flex; align-items: center; gap: 8px;">
            <div style="min-width: 12px; text-align: right; font-size: 0.9em; font-weight: 600; color: var(--error);">✗</div>
            <div style="flex: 1; height: 24px; background: rgba(239, 68, 68, 0.2); border-radius: 4px; overflow: hidden; position: relative;">
                <div style="height: 100%; width: ${failPercentage}%; background: ${failBarColor}; transition: width 0.3s ease; display: flex; align-items: center; justify-content: flex-end; padding-right: 6px;">
                    ${failCount > 0 ? `<span style="font-size: 0.85em; font-weight: 600; color: white;">${failCount}</span>` : ''}
                </div>
                ${failCount === 0 ? `<div style="position: absolute; left: 6px; top: 50%; transform: translateY(-50%); font-size: 0.85em; color: var(--text-secondary);">${failCount}</div>` : ''}
            </div>
        </div>
    `;
    
    chartHTML += '</div>';
    container.innerHTML = chartHTML;
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
        updateAttemptsDistribution();
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
    
    updateAttemptsDistribution();
}

// Strategy and Suggestion Functions
async function changeStrategy() {
    if (!currentGameId) {
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
            console.log('Strategy changed to:', getStrategyDisplayName(strategy));
        } else {
            console.error('Failed to change strategy');
        }
    } catch (error) {
        console.error('Error changing strategy:', error);
    }
}

async function getSuggestion() {
    if (!currentGameId) {
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
            
            // Auto-populate the suggestion into the game grid
            const suggestion = data.suggestion.toUpperCase();
            const inputs = document.querySelectorAll('.letter-input');
            for (let i = 0; i < suggestion.length && i < inputs.length; i++) {
                inputs[i].value = suggestion[i];
                inputs[i].classList.add('filled');
            }
            
            showStatus(`Suggestion populated: ${suggestion} (${data.strategy})`, 'success');
        } else if (response.ok && !data.suggestion) {
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
        case 'DICTIONARY_REDUCTION':
            return 'Dictionary Reduction';
        case 'BELLMAN_OPTIMAL':
            return 'Bellman Optimal';
        case 'BELLMAN_FULL_DICTIONARY':
            return 'Bellman Full Dictionary';
        default:
            return strategy;
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
        
        // Fallback to default option for both selectors
        const selector = document.getElementById('dictionarySelector');
        const selectorDict = document.getElementById('dictionarySelectorDict');
        
        if (selector) {
            selector.innerHTML = '<option value="">Standard (5 Letters)</option>';
        }
        if (selectorDict) {
            selectorDict.innerHTML = '<option value="">Standard (5 Letters)</option>';
        }
    }
}

function populateDictionarySelector() {
    const selector = document.getElementById('dictionarySelector');
    const selectorDict = document.getElementById('dictionarySelectorDict');
    
    // Clear both selectors
    if (selector) selector.innerHTML = '';
    if (selectorDict) selectorDict.innerHTML = '';

    availableDictionaries.forEach(dict => {
        // Create option for Play page selector
        if (selector) {
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
        }
        
        // Create option for Dictionary page selector
        if (selectorDict) {
            const option2 = document.createElement('option');
            option2.value = dict.id;
            option2.textContent = dict.name;
            
            if (!dict.available) {
                option2.disabled = true;
                option2.textContent += ' (Not Available)';
            }
            
            if (dict.description) {
                option2.title = dict.description;
            }
            
            selectorDict.appendChild(option2);
        }
    });

    // Select the 5-letter dictionary by default, fallback to first available
    const fiveLetterDict = availableDictionaries.find(d => d.available && d.wordLength === 5);
    const defaultDict = fiveLetterDict || availableDictionaries.find(d => d.available);
    
    if (defaultDict) {
        if (selector) selector.value = defaultDict.id;
        if (selectorDict) selectorDict.value = defaultDict.id;
        // Adjust the letter input grid for the selected dictionary's word length
        adjustLetterInputGrid(defaultDict.wordLength);
    }

    // Populate Bot Performance dictionary selector
    const analysisSelect = document.getElementById('analysisDictionary');
    if (analysisSelect) {
        analysisSelect.innerHTML = '';
        availableDictionaries.forEach(dict => {
            if (!dict.available) {
                return;
            }
            const option = document.createElement('option');
            option.value = dict.id;
            option.textContent = dict.name;
            if (dict.description) {
                option.title = dict.description;
            }
            analysisSelect.appendChild(option);
        });

        const analysisDefault = availableDictionaries.find(d => d.available && d.wordLength === 5) ||
            availableDictionaries.find(d => d.available);
        if (analysisDefault) {
            analysisSelect.value = analysisDefault.id;
        }
    }

    // Populate Bot Demo dictionary selector based on selected word length
    if (document.getElementById('autoplayWordLength') && document.getElementById('autoplayDictionary')) {
        filterAutoplayDictionaries();
    }

    if (currentView === 'dictionary') {
        refreshDictionaryScreen();
    }
}

function onDictionaryChange() {
    // Get the selector that triggered the change - could be either one
    const selector = document.getElementById('dictionarySelector');
    const selectorDict = document.getElementById('dictionarySelectorDict');
    
    // Determine which selector was changed by checking event.target or use the first non-null value
    let selectedId = null;
    if (selector && selector.value) {
        selectedId = selector.value;
        // Sync the dictionary page selector
        if (selectorDict) selectorDict.value = selectedId;
    } else if (selectorDict && selectorDict.value) {
        selectedId = selectorDict.value;
        // Sync the play page selector
        if (selector) selector.value = selectedId;
    }
    
    const selectedDict = availableDictionaries.find(d => d.id === selectedId);
    
    if (selectedDict) {
        // Adjust the letter input grid for the new word length
        adjustLetterInputGrid(selectedDict.wordLength);

        if (currentView === 'dictionary') {
            refreshDictionaryScreen();
        } else if (currentView === 'play') {
            // Automatically start a new game when dictionary changes on play screen
            newGame().catch(error => {
                console.error('Failed to start new game after dictionary change:', error);
            });
        }
    }
}

// ===== DICTIONARY SCREEN =====

let dictionaryScreenState = {
    loading: false,
    dictionaryId: null
};

function setDictionaryScreenPlaceholders(message) {
    const nameEl = document.getElementById('dictionaryName');
    const lengthEl = document.getElementById('dictionaryWordLength');
    const countEl = document.getElementById('dictionaryWordCount');
    const distEl = document.getElementById('dictionaryComplexityDistribution');
    const freqEl = document.getElementById('dictionaryLetterFrequency');
    const wordsEl = document.getElementById('dictionaryWords');

    if (nameEl) {
        nameEl.textContent = message || 'Select a dictionary from the menu above.';
    }
    if (lengthEl) {
        lengthEl.textContent = '-';
    }
    if (countEl) {
        countEl.textContent = '-';
    }

    const empty = '<p class="muted-center muted-small">No data loaded.</p>';
    if (distEl) {
        distEl.innerHTML = empty;
    }
    if (freqEl) {
        freqEl.innerHTML = empty;
    }
    if (wordsEl) {
        wordsEl.innerHTML = '<p class="muted-center muted-small">No dictionary selected.</p>';
    }
}

function refreshDictionaryScreen() {
    const selector = document.getElementById('dictionarySelector');
    if (!selector || !availableDictionaries || availableDictionaries.length === 0) {
        setDictionaryScreenPlaceholders('Loading dictionaries…');
        return;
    }

    const dictionaryId = selector.value;
    const selectedDict = availableDictionaries.find(d => d.id === dictionaryId);
    if (!dictionaryId || !selectedDict || !selectedDict.available) {
        setDictionaryScreenPlaceholders('Select a dictionary from the menu above.');
        return;
    }

    if (dictionaryScreenState.loading && dictionaryScreenState.dictionaryId === dictionaryId) {
        return;
    }

    dictionaryScreenState.loading = true;
    dictionaryScreenState.dictionaryId = dictionaryId;
    setDictionaryScreenPlaceholders(`Loading ${selectedDict.name}…`);

    loadDictionaryScreenData(dictionaryId, selectedDict.name)
        .catch(err => {
            console.error('Dictionary screen load failed:', err);
            setDictionaryScreenPlaceholders('Failed to load dictionary.');
            showStatus('Failed to load dictionary: ' + err.message, 'error');
        })
        .finally(() => {
            dictionaryScreenState.loading = false;
        });
}

async function loadDictionaryScreenData(dictionaryId, dictionaryName) {
    const response = await fetch(`${API_BASE}/dictionaries/${dictionaryId}`);
    if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
    }

    const data = await response.json();
    console.log('Dictionary data received:', data);
    console.log('Entropy map keys:', data.entropy ? Object.keys(data.entropy).length : 0);
    
    const words = Array.isArray(data.words) ? data.words : [];
    const wordLength = data.wordLength || (words[0] ? words[0].length : 0);
    const entropyMap = data.entropy || {};
    
    console.log('Using entropy map with', Object.keys(entropyMap).length, 'entries');

    const nameEl = document.getElementById('dictionaryName');
    const lengthEl = document.getElementById('dictionaryWordLength');
    const countEl = document.getElementById('dictionaryWordCount');
    if (nameEl) {
        nameEl.textContent = dictionaryName || data.id || dictionaryId;
    }
    if (lengthEl) {
        lengthEl.textContent = String(wordLength || '-');
    }
    if (countEl) {
        countEl.textContent = String(words.length || 0);
    }

    renderDictionaryLetterFrequency(words, wordLength);
    renderDictionaryComplexity(words, wordLength);
    renderDictionaryWords(words, wordLength, entropyMap);
}

// State for letter frequency table sorting
const letterFreqSortState = {
    column: null,
    ascending: true
};

function renderDictionaryLetterFrequency(words, wordLength) {
    const container = document.getElementById('dictionaryLetterFrequency');
    if (!container) {
        return;
    }

    if (!words || words.length === 0 || !wordLength) {
        container.innerHTML = '<p class="muted-center muted-small">No data loaded.</p>';
        return;
    }

    // countsByLetter[letter] = Array(wordLength).fill(count)
    const countsByLetter = new Map();

    for (const word of words) {
        for (let i = 0; i < wordLength; i++) {
            const letter = (word[i] || '').toUpperCase();
            if (!letter) {
                continue;
            }
            if (!countsByLetter.has(letter)) {
                countsByLetter.set(letter, new Array(wordLength).fill(0));
            }
            countsByLetter.get(letter)[i] += 1;
        }
    }

    let letters = Array.from(countsByLetter.keys()).sort();

    // Apply sorting if a column is selected
    if (letterFreqSortState.column !== null) {
        letters = sortLetterFrequencyData(letters, countsByLetter, wordLength, letterFreqSortState.column, letterFreqSortState.ascending);
    }

    // Precompute per-position maxima to drive a temperature-map intensity.
    const maxByPos = new Array(wordLength).fill(0);
    for (const letter of letters) {
        const counts = countsByLetter.get(letter);
        for (let i = 0; i < wordLength; i++) {
            if (counts[i] > maxByPos[i]) {
                maxByPos[i] = counts[i];
            }
        }
    }

    const headerCells = ['Letter'];
    for (let i = 0; i < wordLength; i++) {
        headerCells.push(`Pos ${i + 1}`);
    }
    headerCells.push('Total');

    let html = '<table class="analysis-table">';
    html += '<thead><tr>';
    // Add clickable headers with sort indicators
    headerCells.forEach((h, idx) => {
        const sortClass = letterFreqSortState.column === idx ? (letterFreqSortState.ascending ? 'sort-asc' : 'sort-desc') : '';
        const sortIndicator = letterFreqSortState.column === idx ? (letterFreqSortState.ascending ? ' ▲' : ' ▼') : '';
        html += `<th class="sortable ${sortClass}" onclick="sortLetterFrequency(${idx})" style="cursor: pointer;" title="Click to sort">${h}${sortIndicator}</th>`;
    });
    html += '</tr></thead>';
    html += '<tbody>';

    for (const letter of letters) {
        const counts = countsByLetter.get(letter);
        const total = counts.reduce((sum, n) => sum + n, 0);
        html += `<tr><td class="mono">${letter}</td>`;
        for (let i = 0; i < wordLength; i++) {
            const value = counts[i] || 0;
            const max = maxByPos[i] || 0;
            const ratio = max > 0 ? (value / max) : 0;
            const heat = Math.round(ratio * 90);
            html += `<td class="num heat-cell" style="--heat:${heat}%;" title="${value} occurrences at position ${i + 1}">${value}</td>`;
        }
        html += `<td class="num">${total}</td></tr>`;
    }

    html += '</tbody></table>';
    container.innerHTML = html;
}

function sortLetterFrequencyData(letters, countsByLetter, wordLength, column, ascending) {
    const sorted = [...letters];
    
    sorted.sort((a, b) => {
        let valA, valB;
        
        if (column === 0) {
            // Sort by letter (column 0)
            valA = a;
            valB = b;
            const comparison = valA.localeCompare(valB);
            return ascending ? comparison : -comparison;
        } else if (column === wordLength + 1) {
            // Sort by total (last column)
            const countsA = countsByLetter.get(a);
            const countsB = countsByLetter.get(b);
            valA = countsA.reduce((sum, n) => sum + n, 0);
            valB = countsB.reduce((sum, n) => sum + n, 0);
        } else {
            // Sort by position (columns 1 to wordLength)
            const posIndex = column - 1;
            const countsA = countsByLetter.get(a);
            const countsB = countsByLetter.get(b);
            valA = countsA[posIndex] || 0;
            valB = countsB[posIndex] || 0;
        }
        
        // Numeric comparison for position and total columns
        if (typeof valA === 'number' && typeof valB === 'number') {
            return ascending ? valA - valB : valB - valA;
        }
        
        return 0;
    });
    
    return sorted;
}

function sortLetterFrequency(columnIndex) {
    // Toggle sort direction if clicking the same column
    if (letterFreqSortState.column === columnIndex) {
        letterFreqSortState.ascending = !letterFreqSortState.ascending;
    } else {
        // New column: default to descending for numeric columns, ascending for letter column
        letterFreqSortState.column = columnIndex;
        letterFreqSortState.ascending = columnIndex === 0; // Ascending for letter, descending for numbers
    }
    
    // Re-render the dictionary screen to apply the sort
    refreshDictionaryScreen();
}

function computeOneAwayNeighbourCounts(words, wordLength) {
    // Build pattern counts where one position is replaced by '.'
    const patternCounts = new Map();

    for (const word of words) {
        for (let i = 0; i < wordLength; i++) {
            const pattern = word.substring(0, i) + '.' + word.substring(i + 1);
            patternCounts.set(pattern, (patternCounts.get(pattern) || 0) + 1);
        }
    }

    const neighbourCounts = new Map();
    for (const word of words) {
        let total = 0;
        for (let i = 0; i < wordLength; i++) {
            const pattern = word.substring(0, i) + '.' + word.substring(i + 1);
            total += Math.max(0, (patternCounts.get(pattern) || 0) - 1);
        }
        neighbourCounts.set(word, total);
    }
    return neighbourCounts;
}

function percentile(sortedNumbers, p) {
    if (!sortedNumbers || sortedNumbers.length === 0) {
        return 0;
    }
    const clamped = Math.max(0, Math.min(1, p));
    const idx = Math.floor(clamped * (sortedNumbers.length - 1));
    return sortedNumbers[idx];
}

function getComplexityBucket(score) {
    if (score <= 2) {
        return { key: 'very-easy', label: 'Very Easy', range: '0-2' };
    }
    if (score <= 5) {
        return { key: 'easy', label: 'Easy', range: '3-5' };
    }
    if (score <= 8) {
        return { key: 'medium', label: 'Medium', range: '6-8' };
    }
    if (score <= 11) {
        return { key: 'hard', label: 'Hard', range: '9-11' };
    }
    return { key: 'very-hard', label: 'Very Hard', range: '12-15' };
}

function renderDictionaryComplexity(words, wordLength) {
    const distEl = document.getElementById('dictionaryComplexityDistribution');
    if (!distEl) {
        return;
    }
    if (!words || words.length === 0 || !wordLength) {
        distEl.innerHTML = '<p class="muted-center muted-small">No data loaded.</p>';
        return;
    }

    // Complexity model: number of 1-letter neighbours (matches ComplexityAnalyser's "one-letter" idea)
    const neighbourCounts = computeOneAwayNeighbourCounts(words, wordLength);
    const counts = {
        'very-easy': 0,
        'easy': 0,
        'medium': 0,
        'hard': 0,
        'very-hard': 0
    };
    for (const w of words) {
        const score = neighbourCounts.get(w) || 0;
        counts[getComplexityBucket(score).key]++;
    }

    const total = words.length;
    const pct = (n) => (total > 0 ? Math.round((n / total) * 100) : 0);

    // Create bar chart
    const maxCount = Math.max(counts['very-easy'], counts['easy'], counts['medium'], counts['hard'], counts['very-hard']);
    const chartHeight = 140;
    
    const complexityLevels = [
        { key: 'very-easy', label: 'Very Easy', range: '0-2', color: '#22c55e' },
        { key: 'easy', label: 'Easy', range: '3-5', color: '#84cc16' },
        { key: 'medium', label: 'Medium', range: '6-8', color: '#eab308' },
        { key: 'hard', label: 'Hard', range: '9-11', color: '#f97316' },
        { key: 'very-hard', label: 'Very Hard', range: '12-15', color: '#ef4444' }
    ];

    let chartHTML = '<div class="complexity-bar-chart">';
    
    for (const level of complexityLevels) {
        const count = counts[level.key];
        const heightPercent = maxCount > 0 ? (count / maxCount) * 100 : 0;
        const barHeight = (heightPercent / 100) * chartHeight;
        
        chartHTML += `
            <div class="complexity-bar-container">
                <div class="complexity-bar-wrapper" style="height: ${chartHeight}px;">
                    <div class="complexity-bar" style="
                        height: ${barHeight}px;
                        background: linear-gradient(180deg, ${level.color} 0%, ${level.color}dd 100%);
                    " title="${level.label}: ${count} words (${pct(count)}%)"></div>
                </div>
                <div class="complexity-bar-count">${count}</div>
                <div class="complexity-bar-percent">${pct(count)}%</div>
                <div class="complexity-bar-label">${level.label}</div>
                <div class="complexity-bar-range">${level.range}</div>
            </div>
        `;
    }
    
    chartHTML += '</div>';

    distEl.innerHTML = chartHTML;

    dictionaryScreenState._complexity = { neighbourCounts };
}

// State for dictionary words table sorting and filtering
const dictionaryWordsState = {
    sortColumn: 1, // Default: sort by 1-away (complexity score)
    sortAscending: false, // Default: descending (hardest first)
    searchTerm: '',
    allWords: [],
    wordLength: 0,
    entropyMap: {}
};

function renderDictionaryWords(words, wordLength, entropyMap = {}) {
    const container = document.getElementById('dictionaryWords');
    if (!container) {
        return;
    }
    if (!words || words.length === 0) {
        container.innerHTML = '<p class="muted-center muted-small">No dictionary selected.</p>';
        return;
    }

    console.log('renderDictionaryWords called with', words.length, 'words and entropy map with', Object.keys(entropyMap).length, 'entries');
    if (words.length > 0 && Object.keys(entropyMap).length > 0) {
        const sampleWord = words[0];
        console.log('Sample word:', sampleWord, 'Entropy:', entropyMap[sampleWord]);
    }

    // Store words and entropy for filtering/sorting
    dictionaryWordsState.allWords = words;
    dictionaryWordsState.wordLength = wordLength;
    dictionaryWordsState.entropyMap = entropyMap;

    const complexity = dictionaryScreenState._complexity;
    const neighbourCounts = complexity?.neighbourCounts || new Map();

    let wordInfos = words.map(w => {
        const score = neighbourCounts.get(w) || 0;
        const bucket = getComplexityBucket(score);
        const entropy = entropyMap[w] || 0;
        return { word: w, score, bucket, entropy };
    });

    // Apply search filter
    const searchTerm = dictionaryWordsState.searchTerm.toLowerCase().trim();
    if (searchTerm) {
        wordInfos = wordInfos.filter(info => info.word.toLowerCase().includes(searchTerm));
    }

    // Apply sorting
    wordInfos = sortDictionaryWordsData(wordInfos, dictionaryWordsState.sortColumn, dictionaryWordsState.sortAscending);

    const headers = ['Word', '1-away', 'Complexity', 'Entropy'];
    let html = '<table class="analysis-table">';
    html += '<thead><tr>';
    headers.forEach((h, idx) => {
        const sortClass = dictionaryWordsState.sortColumn === idx ? (dictionaryWordsState.sortAscending ? 'sort-asc' : 'sort-desc') : '';
        const sortIndicator = dictionaryWordsState.sortColumn === idx ? (dictionaryWordsState.sortAscending ? ' ▲' : ' ▼') : '';
        const thClass = (idx === 1 || idx === 3) ? 'num' : ''; // Numeric columns: 1-away and Entropy
        html += `<th class="sortable ${sortClass} ${thClass}" onclick="sortDictionaryWords(${idx})" style="cursor: pointer;" title="Click to sort">${h}${sortIndicator}</th>`;
    });
    html += '</tr></thead>';
    html += '<tbody>';
    
    if (wordInfos.length === 0) {
        html += '<tr><td colspan="4" class="muted-center" style="padding: 20px;">No words match your search</td></tr>';
    } else {
        for (const info of wordInfos) {
            html += `
                <tr>
                    <td class="mono">${info.word.toUpperCase()}</td>
                    <td class="num">${info.score}</td>
                    <td><span class="complexity-pill complexity-${info.bucket.key}">${info.bucket.label}</span></td>
                    <td class="num">${info.entropy.toFixed(3)}</td>
                </tr>
            `;
        }
    }
    html += '</tbody></table>';
    container.innerHTML = html;

    // After rendering, match right column height to left column height
    matchDictionaryColumnHeights();
}

function sortDictionaryWordsData(wordInfos, column, ascending) {
    const sorted = [...wordInfos];
    
    sorted.sort((a, b) => {
        let valA, valB;
        
        if (column === 0) {
            // Sort by Word
            valA = a.word;
            valB = b.word;
            const comparison = valA.localeCompare(valB);
            return ascending ? comparison : -comparison;
        } else if (column === 1) {
            // Sort by 1-away score
            valA = a.score;
            valB = b.score;
        } else if (column === 2) {
            // Sort by Complexity bucket
            const complexityOrder = { 'very-easy': 0, 'easy': 1, 'medium': 2, 'hard': 3, 'very-hard': 4 };
            valA = complexityOrder[a.bucket.key] || 0;
            valB = complexityOrder[b.bucket.key] || 0;
        } else if (column === 3) {
            // Sort by Entropy
            valA = a.entropy || 0;
            valB = b.entropy || 0;
        }
        
        // Numeric comparison
        if (typeof valA === 'number' && typeof valB === 'number') {
            return ascending ? valA - valB : valB - valA;
        }
        
        return 0;
    });
    
    return sorted;
}

function sortDictionaryWords(columnIndex) {
    // Toggle sort direction if clicking the same column
    if (dictionaryWordsState.sortColumn === columnIndex) {
        dictionaryWordsState.sortAscending = !dictionaryWordsState.sortAscending;
    } else {
        // New column: default to descending for numeric columns, ascending for word column
        dictionaryWordsState.sortColumn = columnIndex;
        dictionaryWordsState.sortAscending = columnIndex === 0; // Ascending for word, descending for numbers
    }
    
    // Re-render with current words and entropy data
    renderDictionaryWords(dictionaryWordsState.allWords, dictionaryWordsState.wordLength, dictionaryWordsState.entropyMap);
}

function filterDictionaryWords() {
    const searchInput = document.getElementById('dictionaryWordsSearch');
    if (searchInput) {
        dictionaryWordsState.searchTerm = searchInput.value;
        renderDictionaryWords(dictionaryWordsState.allWords, dictionaryWordsState.wordLength, dictionaryWordsState.entropyMap);
    }
}

function matchDictionaryColumnHeights() {
    const leftPanel = document.querySelector('.dictionary-col-left .dictionary-panel');
    const rightCol = document.querySelector('.dictionary-col-right');
    
    if (!leftPanel || !rightCol) {
        return;
    }

    // Reset right column height first
    rightCol.style.height = '';
    
    // Get natural height of left panel (Letter Frequency panel)
    const leftPanelHeight = leftPanel.offsetHeight;
    
    // Set right column to match the panel height (not the column height)
    rightCol.style.height = `${leftPanelHeight}px`;
}

function adjustLetterInputGrid(wordLength) {
    const letterInputs = document.getElementById('letterInputs');
    letterInputs.innerHTML = '';
    const suppressKeyboard = shouldSuppressNativeKeyboard();

    for (let i = 0; i < wordLength; i++) {
        const input = document.createElement('input');
        input.type = 'text';
        input.className = 'letter-input';
        input.maxLength = 1;
        input.id = `letter${i}`;
        input.autocomplete = 'off';
        input.spellcheck = false;
        input.inputMode = suppressKeyboard ? 'none' : 'text';
        input.autocapitalize = suppressKeyboard ? 'off' : 'characters';
        input.setAttribute('autocorrect', 'off');
        input.setAttribute('aria-label', `Letter ${i + 1} of ${wordLength}`);
        if (suppressKeyboard) {
            input.setAttribute('readonly', 'readonly');
        }
        
        letterInputs.appendChild(input);
    }

    // Attach the standard handlers (filled state, key filtering, Enter/backspace behavior)
    setupLetterInputs();
    
    // Focus the first input
    if (!suppressKeyboard && wordLength > 0) {
        document.getElementById('letter0').focus();
    }
    
    // Initialize keyboard after setting up inputs
    initializeKeyboard();
}

/* ========================================
   ON-SCREEN KEYBOARD FUNCTIONS
   ======================================== */

function initializeKeyboard() {
    const keyboardContainer = document.getElementById('gameKeyboard');
    if (!keyboardContainer) {
        return;
    }
    
    // Clear existing keyboard
    keyboardContainer.innerHTML = '';
    
    // Initialize keyboard state tracking
    window.keyboardState = window.keyboardState || {};
    
    // Create standard QWERTY layout in rows
    const rows = [
        ['Q','W','E','R','T','Y','U','I','O','P'],
        ['A','S','D','F','G','H','J','K','L'],
        ['Z','X','C','V','B','N','M'] // Backspace/Enter handled separately or added here
    ];
    
    rows.forEach((rowKeys, rowIndex) => {
        const rowDiv = document.createElement('div');
        rowDiv.className = 'keyboard-row';
        
        rowKeys.forEach(letter => {
            const button = document.createElement('button');
            button.type = 'button';
            // Use 'game-key' to match new CSS
            button.className = 'game-key key-unused';
            button.textContent = letter;
            button.dataset.letter = letter;
            button.setAttribute('aria-label', `Letter ${letter}`);
            
            // Handle click on keyboard
            button.addEventListener('click', function(e) {
                e.preventDefault();
                // Vibration feedback if supported
                if (navigator.vibrate) navigator.vibrate(5);
                
                if (!this.disabled && !gameEnded) {
                    insertLetterFromKeyboard(letter);
                }
            });
            
            rowDiv.appendChild(button);
        });
        
        // Add special keys to bottom row
        if (rowIndex === 2) {
            // ENTER key on the left — also serves as the Make Guess button
            const enterBtn = document.createElement('button');
            enterBtn.id = 'guessBtn';
            enterBtn.type = 'button';
            enterBtn.className = 'game-key wide-key enter-key';
            enterBtn.textContent = 'ENTER';
            enterBtn.setAttribute('aria-label', 'Enter / Make Guess');
            enterBtn.addEventListener('click', function(e) {
                e.preventDefault();
                if (!this.disabled && !gameEnded) makeGuess();
            });
            rowDiv.insertBefore(enterBtn, rowDiv.firstChild);

            // Backspace on the right
            const backspaceBtn = document.createElement('button');
            backspaceBtn.type = 'button';
            backspaceBtn.className = 'game-key wide-key';
            backspaceBtn.innerHTML = '⌫';
            backspaceBtn.setAttribute('aria-label', 'Backspace');
            backspaceBtn.addEventListener('click', function(e) {
                e.preventDefault();
                if (!gameEnded) deleteLastLetter();
            });
            rowDiv.appendChild(backspaceBtn);
        }
        
        keyboardContainer.appendChild(rowDiv);
    });
    
    // Reset keyboard state for new game
    updateKeyboardDisplay({});
}

function deleteLastLetter() {
    const inputs = document.querySelectorAll('.letter-input');
    // Find the last filled input
    let lastFilledIndex = -1;
    for (let i = 0; i < inputs.length; i++) {
        if (inputs[i].value) {
            lastFilledIndex = i;
        } else {
            break; 
        }
    }
    
    if (lastFilledIndex >= 0) {
        const input = inputs[lastFilledIndex];
        input.value = '';
        input.classList.remove('filled');
        input.focus();
    } else if (inputs.length > 0) {
        // If nothing filled, ensure first is focused
        inputs[0].focus();
    }
}

function insertLetterFromKeyboard(letter) {
    const inputs = document.querySelectorAll('.letter-input');
    const suppressKeyboard = shouldSuppressNativeKeyboard();
    let inserted = false;
    
    for (let i = 0; i < inputs.length; i++) {
        if (!inputs[i].value) {
            inputs[i].value = letter;
            inputs[i].classList.add('filled');
            if (!suppressKeyboard) {
                inputs[i].focus();
            }
            inserted = true;
            break;
        }
    }
    
    // If all filled, focus last input
    if (!inserted && inputs.length > 0 && !suppressKeyboard) {
        inputs[inputs.length - 1].focus();
    }
}

function updateKeyboardDisplay(responseCounts) {
    const keyboardContainer = document.getElementById('gameKeyboard');
    if (!keyboardContainer) {
        return;
    }
    
    // Select all keys (including backspace/enter if they have game-key class)
    const buttons = keyboardContainer.querySelectorAll('.game-key');
    
    buttons.forEach(button => {
        const letter = (button.dataset.letter || '').toUpperCase();
        if (!letter) return; // Skip special keys like Backspace that might not have dataset.letter

        const status = responseCounts ? responseCounts[letter] : undefined;
        
        // Check the response status for this letter from all previous guesses
        if (status === 'G') {
            // Green - correct position
            button.className = 'game-key key-correct';
        } else if (status === 'A') {
            // Amber/Yellow - wrong position
            button.className = 'game-key key-present';
        } else if (status === 'R' || status === 'X') {
            // Red - absent or excess
            button.className = 'game-key key-absent';
        } else {
            // Not yet guessed
            button.className = 'game-key key-unused';
        }
    });
}

function trackLetterStatus(results) {
    // Track best status for each letter across all guesses
    // G (correct) > A (present) > R/X (absent) > unused
    
    if (!window.letterStatusMap) {
        window.letterStatusMap = {};
    }
    
    if (!results || !Array.isArray(results)) {
        return;
    }
    
    results.forEach(result => {
        const letter = (result.letter || '').toUpperCase();
        const status = result.status;
        
        const currentStatus = window.letterStatusMap[letter] || null;
        
        // Update to best status found
        if (status === 'G') {
            window.letterStatusMap[letter] = 'G';
        } else if (status === 'A' && currentStatus !== 'G') {
            window.letterStatusMap[letter] = 'A';
        } else if ((status === 'R' || status === 'X') && !currentStatus) {
            window.letterStatusMap[letter] = 'R';
        }
    });
    
    updateKeyboardDisplay(window.letterStatusMap);
}


// Session Viewer Functions
function showSessionViewer() {
    if (window.location.hash !== '#/session') {
        window.location.hash = '#/session';
    }
    renderSessionDetails();
}

function hideSessionViewer() {
    if (window.location.hash !== '#/play') {
        window.location.hash = '#/play';
    }
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
            <div style="background: white; color: var(--bg-primary); border-radius: 12px; padding: 20px; margin-bottom: 20px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); border-left: 5px solid ${game.won ? 'var(--success-green)' : 'var(--error-red)'}; max-width: 100%; overflow: hidden; box-sizing: border-box;">
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
                    <div style="overflow-x: auto; -webkit-overflow-scrolling: touch; width: 100%;">
                    <table style="width: 100%; min-width: 340px; border-collapse: collapse;">
                        <thead>
                            <tr style="background: #f1f5f9; color: var(--bg-primary); font-size: 0.8em; text-transform: uppercase;">
                                <th style="padding: 6px 8px; text-align: left; border-bottom: 2px solid #cbd5e1; white-space: nowrap;">#</th>
                                <th style="padding: 6px 8px; text-align: left; border-bottom: 2px solid #cbd5e1; white-space: nowrap;">Guess</th>
                                <th style="padding: 6px 8px; text-align: left; border-bottom: 2px solid #cbd5e1; white-space: nowrap;">Response</th>
                                <th style="padding: 6px 8px; text-align: right; border-bottom: 2px solid #cbd5e1; white-space: nowrap;">Rem.</th>
                                <th style="padding: 6px 8px; text-align: right; border-bottom: 2px solid #cbd5e1; white-space: nowrap;">Red.%</th>
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
                    
                    responseHtml += `<div style="width: 24px; height: 24px; background: ${bgColor}; color: white; display: flex; align-items: center; justify-content: center; border-radius: 3px; font-weight: 700; font-size: 0.75em; font-family: var(--font-mono);">${result.letter}</div>`;
                });
                responseHtml += '</div>';

                html += `
                    <tr style="border-bottom: 1px solid #e2e8f0; color: var(--bg-primary);">
                        <td style="padding: 6px 8px; font-weight: 700; color: var(--bg-primary); white-space: nowrap;">#${guessData.attempt}</td>
                        <td style="padding: 6px 8px; font-family: var(--font-mono); font-weight: 700; font-size: 1em; white-space: nowrap;">${guessData.guess.toUpperCase()}</td>
                        <td style="padding: 6px 8px;">${responseHtml}</td>
                        <td style="padding: 6px 8px; text-align: right; font-family: var(--font-mono); font-weight: 600; white-space: nowrap;">${guessData.remainingWords || 'N/A'}</td>
                        <td style="padding: 6px 8px; text-align: right; font-family: var(--font-mono); font-weight: 600; color: var(--success-green); white-space: nowrap;">${reductionPercent}%</td>
                    </tr>
                `;

                // Add dictionary metrics if available
                if (guessData.dictionaryMetrics) {
                    html += `
                        <tr style="background: rgba(33, 150, 243, 0.05);">
                            <td colspan="5" style="padding: 6px 8px; font-size: 0.8em; word-break: break-word; overflow-wrap: anywhere;">
                                <strong>Metrics:</strong> 
                                Unique: ${guessData.dictionaryMetrics.uniqueCharacters || 'N/A'} | 
                                Letters: ${guessData.dictionaryMetrics.letterCount || 'N/A'}
                                ${guessData.dictionaryMetrics.columnLengths ? ' | Cols: [' + guessData.dictionaryMetrics.columnLengths.join(', ') + ']' : ''}
                            </td>
                        </tr>
                    `;
                }
            });

            html += `
                        </tbody>
                    </table>
                    </div>
                </div>
            `;
        } else {
            html += `<div style="color: #888; font-style: italic; margin-top: 10px;">Detailed guess data not available for this game.</div>`;
        }

        html += `</div>`;
    });

    contentDiv.innerHTML = html;
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
    // Screen-based UI: keep user selections; ensure dictionaries are populated
    const wordLengthEl = document.getElementById('autoplayWordLength');
    if (wordLengthEl && !wordLengthEl.value) {
        wordLengthEl.value = '5';
    }
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
    // Modal removed in Phase 2 (screen-based UI)
}

function toggleAutoplay() {
    if (autoplayState.isRunning) {
        // Stop autoplay
        stopAutoplay();
    } else {
        // Ensure selectors are populated, then start
        showAutoplayModal();
        startAutoplay();
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
    if (!autoplayBtn) {
        return;
    }
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

function updateAutoplayProgress(text, percent) {
    const progressText = document.getElementById('autoplayProgressText');
    const progressBar = document.getElementById('autoplayProgressBar');
    if (progressText) {
        progressText.textContent = text;
    }
    if (progressBar) {
        progressBar.value = Math.max(0, Math.min(100, percent));
    }
}

function setBotDemoRunningUi(isRunning) {
    const botDemoScreen = document.getElementById('screen-bot-demo');
    if (botDemoScreen) {
        botDemoScreen.classList.toggle('bot-demo-running', !!isRunning);
    }

    const setupPanel = document.getElementById('autoplaySetupPanel');
    if (setupPanel && setupPanel.tagName === 'DETAILS' && isRunning) {
        setupPanel.open = false;
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
    
    // Screen-based UI: no modal to hide
    
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
        
        const globalDictSelector = document.getElementById('dictionarySelector');
        if (globalDictSelector) {
            globalDictSelector.disabled = true;
        }
        const guessBtn = document.getElementById('guessBtn');
        if (guessBtn) {
            guessBtn.disabled = true;
        }
        
        updateAutoplayButton(true);

        setBotDemoRunningUi(true);

        updateAutoplayProgress(`Starting: 0/${gameCount} games`, 0);
        
        showStatus(`Starting autoplay: ${gameCount} ${wordLength}-letter games with ${strategy} strategy (${guessDelay}ms delay)`, 'success');
        
        await runAutoplayGames(selectedDict, strategy, wordLength, guessDelay);
        
        autoplayState.isRunning = false;
        autoplayState.shouldStop = false;
        setBotDemoRunningUi(false);
        if (globalDictSelector) {
            globalDictSelector.disabled = false;
        }
        if (guessBtn) {
            guessBtn.disabled = false;
        }
        
        updateAutoplayButton(false);

        const completed = autoplayState.gamesCompleted;
        const pct = gameCount > 0 ? Math.round((completed / gameCount) * 100) : 0;
        updateAutoplayProgress(`Completed: ${completed}/${gameCount} games`, pct);
        
        const statusMessage = autoplayState.gamesCompleted < gameCount 
            ? `Autoplay stopped after ${autoplayState.gamesCompleted} games. Displaying session statistics...`
            : 'Autoplay completed! Displaying session statistics...';
        showStatus(statusMessage, 'success');
        showSessionViewer();
        
    } catch (error) {
        autoplayState.isRunning = false;
        autoplayState.shouldStop = false;
        setBotDemoRunningUi(false);
        const globalDictSelector = document.getElementById('dictionarySelector');
        if (globalDictSelector) {
            globalDictSelector.disabled = false;
        }
        const guessBtn = document.getElementById('guessBtn');
        if (guessBtn) {
            guessBtn.disabled = false;
        }
        updateAutoplayButton(false);
        updateAutoplayProgress('Failed', 0);
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
        const pct = autoplayState.gameCount > 0 ? Math.round((i / autoplayState.gameCount) * 100) : 0;
        updateAutoplayProgress(`Running: ${i + 1}/${autoplayState.gameCount} games`, pct);
        
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
            window.letterStatusMap = {};
            latestOccurrenceData = null;
            
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

            autoplayState.gamesCompleted = i + 1;
            const pctDone = autoplayState.gameCount > 0 ? Math.round(((i + 1) / autoplayState.gameCount) * 100) : 0;
            updateAutoplayProgress(`Completed: ${i + 1}/${autoplayState.gameCount} games`, pctDone);
            
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

// ===== PLAYER ANALYSIS FUNCTIONALITY =====

// Player analysis state
const analysisState = {
    running: false,
    shouldStop: false,
    completed: 0,
    total: 0,
    summaryData: [],
    detailsData: []
};

function updateAnalysisProgress(text, percent) {
    const progressText = document.getElementById('analysisProgressText');
    const progressBar = document.getElementById('analysisProgressBar');
    if (progressText) {
        progressText.textContent = text;
    }
    if (progressBar) {
        progressBar.value = Math.max(0, Math.min(100, percent));
    }
}

function cancelPlayerAnalysis() {
    if (!analysisState.running) {
        return;
    }
    analysisState.shouldStop = true;
    const cancelBtn = document.getElementById('cancelAnalysisBtn');
    if (cancelBtn) {
        cancelBtn.textContent = 'Cancelling...';
        cancelBtn.disabled = true;
    }
    showStatus('Cancelling analysis after the current word completes...', 'info');
}

function setBotPerformanceRunningUi(isRunning) {
    const performanceScreen = document.getElementById('screen-bot-performance');
    if (performanceScreen) {
        performanceScreen.classList.toggle('bot-performance-running', !!isRunning);
    }

    const setupPanel = document.getElementById('analysisSetupPanel');
    if (setupPanel && setupPanel.tagName === 'DETAILS' && isRunning) {
        setupPanel.open = false;
    }
}

function showAnalysisModal() {
    const dictionarySelect = document.getElementById('analysisDictionary');
    dictionarySelect.innerHTML = '';
    availableDictionaries.forEach(dict => {
        if (dict.available) {
            const option = document.createElement('option');
            option.value = dict.id;
            option.textContent = dict.name;
            if (dict.wordLength === 5) { option.selected = true; }
            dictionarySelect.appendChild(option);
        }
    });
}

function hideAnalysisModal() {
    // Modal removed in Phase 2 (screen-based UI)
}

async function startPlayerAnalysis() {
    const strategy = document.getElementById('analysisStrategy').value;
    const dictionaryId = document.getElementById('analysisDictionary').value;
    const guessDelay = parseInt(document.getElementById('analysisDelay').value) || 0;

    const startBtn = document.getElementById('startAnalysisBtn');
    const cancelBtn = document.getElementById('cancelAnalysisBtn');
    if (startBtn) {
        startBtn.disabled = true;
    }
    if (cancelBtn) {
        cancelBtn.disabled = false;
        cancelBtn.textContent = 'Cancel';
    }
    
    // Get all words from the dictionary
    try {
        setBotPerformanceRunningUi(true);

        const dictResponse = await fetch(`${API_BASE}/dictionaries/${dictionaryId}`);
        if (!dictResponse.ok) throw new Error('Failed to load dictionary');
        
        const dictData = await dictResponse.json();
        const allWords = dictData.words || [];
        
        analysisState.running = true;
        analysisState.shouldStop = false;
        analysisState.completed = 0;
        analysisState.total = allWords.length;
        analysisState.summaryData = [];
        analysisState.detailsData = [];

        updateAnalysisProgress(`Starting: 0/${allWords.length}`, 0);
        
        showStatus(`Starting Player Analysis: Testing ${allWords.length} words with ${strategy}`, 'info');
        
        // Run games for each word in dictionary
        await runAnalysisGames(allWords, dictionaryId, strategy, dictData.wordLength, guessDelay);
        
        // Show results
        displayFinalAnalysisResults();
        
    } catch (error) {
        console.error('Player Analysis error:', error);
        showStatus('Analysis failed: ' + error.message, 'error');
        updateAnalysisProgress('Failed', 0);
    } finally {
        analysisState.running = false;
        setBotPerformanceRunningUi(false);

        if (startBtn) {
            startBtn.disabled = false;
        }
        if (cancelBtn) {
            cancelBtn.disabled = true;
            cancelBtn.textContent = 'Cancel';
        }
    }
}

async function runAnalysisGames(targetWords, dictionaryId, strategy, wordLength, guessDelay) {
    for (let i = 0; i < targetWords.length; i++) {
        if (analysisState.shouldStop) {
            showStatus(`Analysis cancelled at ${analysisState.completed}/${analysisState.total}`, 'warning');
            break;
        }

        const targetWord = targetWords[i];
        analysisState.completed = i + 1;

        const pct = analysisState.total > 0 ? Math.round((analysisState.completed / analysisState.total) * 100) : 0;
        updateAnalysisProgress(`Running: ${analysisState.completed}/${analysisState.total} ("${targetWord.toUpperCase()}")`, pct);
        
        try {
            // Create game with specific target word
            const createResponse = await fetch(`${API_BASE}/games`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ dictionaryId: dictionaryId, targetWord: targetWord })
            });
            
            if (!createResponse.ok) throw new Error('Failed to create game');
            
            const gameData = await createResponse.json();
            const gameId = gameData.gameId;
            
            // During analysis, do not update gameplay UI (board/history/analytics).
            // Keep only status/progress updates.
            
            await fetch(`${API_BASE}/games/${gameId}/strategy`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ strategy: strategy })
            });
            
            showStatus(`Analysis: ${i + 1}/${targetWords.length} - Testing "${targetWord.toUpperCase()}" with ${strategy}`, 'info');
            
            // Play the game
            const gameResult = await playAnalysisGame(gameId, targetWord, strategy, gameData.maxAttempts || 6);
            
            // Store results
            analysisState.summaryData.push(gameResult.summary);
            analysisState.detailsData.push(...gameResult.details);
            
            // Clean up game session
            try {
                await fetch(`${API_BASE}/games/${gameId}`, { method: 'DELETE' });
            } catch (e) {
                console.warn('Failed to delete game session:', e);
            }
            
            if (guessDelay > 0) {
                await new Promise(resolve => setTimeout(resolve, guessDelay));
            }
            
        } catch (error) {
            console.error(`Error analyzing word "${targetWord}":`, error);
            // Continue with next word
        }
    }
}

async function playAnalysisGame(gameId, targetWord, strategy, maxAttempts = 6) {
    let attempt = 0;
    let won = false;
    let localGameEnded = false;
    const details = [];
    const guesses = [];
    
    while (!localGameEnded && attempt < maxAttempts) {
        try {
            const suggestionResponse = await fetch(`${API_BASE}/games/${gameId}/suggestion`);
            if (!suggestionResponse.ok) {
                console.error('Failed to get suggestion');
                break;
            }
            
            const suggestionData = await suggestionResponse.json();
            const suggestedWord = suggestionData.suggestion;
            
            if (!suggestedWord) {
                console.error('No suggestion returned');
                break;
            }
            
            const guessResponse = await fetch(`${API_BASE}/games/${gameId}/guess`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ word: suggestedWord.toLowerCase() })
            });
            
            if (!guessResponse.ok) {
                const errorData = await guessResponse.json();
                console.error('Guess failed:', errorData.message);
                break;
            }
            
            const guessData = await guessResponse.json();
            attempt = guessData.attemptNumber;

            // Collect guess data (no UI updates during analysis)
            guesses.push(suggestedWord.toUpperCase());
            
            // Store detail data
            details.push({
                iteration: analysisState.completed,
                attempt: attempt,
                targetWord: targetWord.toUpperCase(),
                guess: suggestedWord.toUpperCase(),
                remainingWords: guessData.remainingWordsCount || 0,
                letterCount: suggestedWord.length,
                winner: guessData.gameWon ? 'true' : 'false'
            });
            
            if (guessData.gameWon) {
                won = true;
                localGameEnded = true;
                break;
            }
            
            if (guessData.gameOver) {
                localGameEnded = true;
                break;
            }
            
        } catch (error) {
            console.error('Error during guess:', error);
            break;
        }
    }
    
    const guessesStr = guesses.join(',');
    
    return {
        summary: {
            iteration: analysisState.completed,
            targetWord: targetWord.toUpperCase(),
            algorithm: strategy,
            attempts: attempt,
            guesses: guessesStr,
            result: won ? 'WON' : 'LOST'
        },
        details: details
    };
}

function displayFinalAnalysisResults() {
    // Calculate statistics
    const totalGames = analysisState.summaryData.length;
    const gamesWon = analysisState.summaryData.filter(g => g.result === 'WON').length;
    const gamesLost = totalGames - gamesWon;
    const winRate = totalGames > 0 ? (gamesWon * 100.0 / totalGames) : 0;
    
    const wonAttempts = analysisState.summaryData.filter(g => g.result === 'WON').map(g => g.attempts);
    const minAttempts = wonAttempts.length > 0 ? Math.min(...wonAttempts) : null;
    const maxAttempts = wonAttempts.length > 0 ? Math.max(...wonAttempts) : null;
    const avgAttempts = wonAttempts.length > 0 ? wonAttempts.reduce((a, b) => a + b, 0) / wonAttempts.length : null;
    
    // Show results section
    document.getElementById('analysisResults').style.display = 'block';
    
    // Build summary table
    const winRateDisplay = winRate === 100 ? '100' : winRate.toFixed(2);
    const avgDisplay = avgAttempts ? avgAttempts.toFixed(2) : 'N/A';
    
    const summaryHTML = `
        <table class="analysis-table">
            <thead>
                <tr>
                    <th>Metric</th>
                    <th class="num">Value</th>
                </tr>
            </thead>
            <tbody>
                <tr><td>Total Games</td><td class="num">${totalGames}</td></tr>
                <tr><td>Games Won</td><td class="num">${gamesWon}</td></tr>
                <tr><td>Games Lost</td><td class="num">${gamesLost}</td></tr>
                <tr><td>Win Rate</td><td class="num">${winRateDisplay}%</td></tr>
                <tr><td>Min Attempts (Won)</td><td class="num">${minAttempts||'N/A'}</td></tr>
                <tr><td>Avg Attempts (Won)</td><td class="num">${avgDisplay}</td></tr>
                <tr><td>Max Attempts (Won)</td><td class="num">${maxAttempts||'N/A'}</td></tr>
            </tbody>
        </table>
    `;
    document.getElementById('analysisSummary').innerHTML = summaryHTML;
    
    // Build details table
    let detailsHTML = `
        <table class="analysis-table">
            <thead>
                <tr>
                    <th>Iteration</th>
                    <th>Target</th>
                    <th class="num">Attempts</th>
                    <th>Result</th>
                </tr>
            </thead>
            <tbody>
    `;
    
    analysisState.summaryData.forEach((game, idx) => {
        const resultText = game.result === 'WON' ? '✓ WON' : '✗ LOST';
        const resultClass = game.result === 'WON' ? 'won' : 'lost';
        detailsHTML += `
            <tr>
                <td>${game.iteration}</td>
                <td class="mono">${game.targetWord.toUpperCase()}</td>
                <td class="num">${game.attempts}</td>
                <td><span class="analysis-pill ${resultClass}">${resultText}</span></td>
            </tr>
        `;
    });
    
    detailsHTML += '</tbody></table>';
    document.getElementById('analysisDetails').innerHTML = detailsHTML;
    
    showStatus(`Analysis complete! ${totalGames} games tested with ${winRateDisplay}% win rate`, 'success');

    updateAnalysisProgress(`Completed: ${totalGames}/${analysisState.total}`, 100);
    
    // Scroll to results
    document.getElementById('analysisResults').scrollIntoView({ behavior: 'smooth' });
}

function hideAnalysisResults() {
    document.getElementById('analysisResults').style.display = 'none';
}

function downloadAnalysisSummary() {
    if (analysisState.summaryData.length === 0) {
        showStatus('No analysis data to download', 'error');
        return;
    }
    
    let csv = 'Iteration,TargetWord,Algorithm,Attempts,Guesses,Result\n';
    analysisState.summaryData.forEach(game => {
        csv += `${game.iteration},${game.targetWord},${game.algorithm},${game.attempts},"${game.guesses}",${game.result}\n`;
    });
    
    downloadCSV(csv, 'player-analysis-summary.csv');
}

function downloadAnalysisDetails() {
    if (analysisState.detailsData.length === 0) {
        showStatus('No analysis details to download', 'error');
        return;
    }
    
    let csv = 'Iteration,Attempt,TargetWord,Guess,RemainingWords,LetterCount,Winner\n';
    analysisState.detailsData.forEach(detail => {
        csv += `${detail.iteration},${detail.attempt},${detail.targetWord},${detail.guess},${detail.remainingWords},${detail.letterCount},${detail.winner}\n`;
    });
    
    downloadCSV(csv, 'player-analysis-details.csv');
}

function downloadCSV(content, filename) {
    const blob = new Blob([content], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', filename);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}

// ========================================
// MOBILE NAVIGATION
// ========================================

function initMobileNavigation() {
    const hamburgerMenu = document.getElementById('hamburgerMenu');
    const appNav = document.getElementById('appNav');
    
    if (!hamburgerMenu || !appNav) {
        return;
    }
    
    // Create overlay for mobile menu
    const overlay = document.createElement('div');
    overlay.className = 'mobile-nav-overlay';
    overlay.id = 'mobileNavOverlay';
    document.body.appendChild(overlay);
    
    // Toggle menu on hamburger click
    hamburgerMenu.addEventListener('click', function(e) {
        e.stopPropagation();
        toggleMobileNav();
    });
    
    // Close menu when overlay is clicked
    overlay.addEventListener('click', function() {
        closeMobileNav();
    });
    
    // Close menu and navigate when a nav link is clicked
    const navLinks = appNav.querySelectorAll('.nav-link');
    navLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            const nav = this.getAttribute('data-nav');
            closeMobileNav();
            if (nav) {
                e.preventDefault(); // don't let the browser handle the hash change
                window.location.hash = '#/' + nav;
                setView(nav);      // drive navigation explicitly
            }
        });
    });
    
    // Close menu on escape key
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape' && appNav.classList.contains('mobile-nav-open')) {
            closeMobileNav();
        }
    });
    
    // Initialize mobile view switcher for progressive disclosure
    initMobileViewSwitcher();
}

function toggleMobileNav() {
    const hamburgerMenu = document.getElementById('hamburgerMenu');
    const appNav = document.getElementById('appNav');
    const overlay = document.getElementById('mobileNavOverlay');

    if (!hamburgerMenu || !appNav || !overlay) {
        return;
    }
    
    const isOpen = appNav.classList.toggle('mobile-nav-open');
    hamburgerMenu.classList.toggle('active');
    overlay.classList.toggle('active');
    
    // Update aria-expanded
    hamburgerMenu.setAttribute('aria-expanded', isOpen);
    
    // Prevent body scroll when menu is open
    if (isOpen) {
        document.body.style.overflow = 'hidden';
    } else {
        document.body.style.overflow = '';
    }
}

function closeMobileNav() {
    const hamburgerMenu = document.getElementById('hamburgerMenu');
    const appNav = document.getElementById('appNav');
    const overlay = document.getElementById('mobileNavOverlay');

    if (!hamburgerMenu || !appNav || !overlay) {
        return;
    }
    
    appNav.classList.remove('mobile-nav-open');
    hamburgerMenu.classList.remove('active');
    overlay.classList.remove('active');
    hamburgerMenu.setAttribute('aria-expanded', 'false');
    document.body.style.overflow = '';
}

/* ========================================
   MOBILE VIEW SWITCHING - PROGRESSIVE DISCLOSURE
   ======================================== */

function initMobileViewSwitcher() {
    const navBtns = document.querySelectorAll('.mobile-nav-btn[data-panel]');
    navBtns.forEach(btn => {
        btn.addEventListener('click', function() {
            const panel = parseInt(this.getAttribute('data-panel'), 10);
            if (!isNaN(panel)) switchMobilePanel(panel);
        });
    });

    handleMobilePanelMode();

    window.addEventListener('resize', handleMobilePanelMode);
}

function handleMobilePanelMode() {
    if (window.innerWidth >= 769) {
        showAllPanels();
        return;
    }
    // Only activate play-screen panels when actually on the play screen.
    // On all other screens the !important display rules would bleed game panels
    // over the target screen content, blocking interaction.
    if (currentView !== 'play') {
        document.querySelectorAll('.history-panel, .game-container, .info-panel').forEach(el => {
            el.classList.remove('mobile-active');
        });
        document.querySelectorAll('[data-mobile-panel]').forEach(el => {
            el.classList.remove('mobile-panel-active');
        });
        return;
    }
    switchMobilePanel(currentMobilePanel);
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
function switchMobilePanel(n) {
    if (window.innerWidth >= 769) return;

    currentMobilePanel = n;
    currentMobileView = (n === 1) ? 'game' : (n <= 4) ? 'assistant' : 'session'; // keep legacy in sync

    // Update nav button active states
    document.querySelectorAll('.mobile-nav-btn[data-panel]').forEach(btn => {
        const isActive = parseInt(btn.getAttribute('data-panel'), 10) === n;
        btn.classList.toggle('active', isActive);
        btn.setAttribute('aria-current', isActive ? 'page' : 'false');
    });

    // Clear all column panel active states
    const historyPanel   = document.querySelector('.history-panel');
    const gameContainer  = document.querySelector('.game-container');
    const infoPanel      = document.querySelector('.info-panel');
    historyPanel?.classList.remove('mobile-active');
    gameContainer?.classList.remove('mobile-active');
    infoPanel?.classList.remove('mobile-active');

    // Clear all sub-panel active states
    document.querySelectorAll('[data-mobile-panel]').forEach(el => {
        el.classList.remove('mobile-panel-active');
    });

    // Activate parent column panel
    const parentMap = {
        1: gameContainer,
        2: infoPanel, 3: infoPanel, 4: infoPanel,
        5: historyPanel, 6: historyPanel, 7: historyPanel
    };
    const parent = parentMap[n];
    if (parent) {
        parent.classList.add('mobile-active');
        parent.scrollTop = 0;
    }

    // Activate sub-panel (panels 2-7 each have a [data-mobile-panel] section)
    if (n >= 2) {
        const subPanel = document.querySelector(`[data-mobile-panel="${n}"]`);
        if (subPanel) {
            subPanel.classList.add('mobile-panel-active');
            subPanel.scrollTop = 0;
        }
    }
}

/** Backward-compat alias — kept so any external/legacy call still works. */
function switchMobileView(view) {
    const viewToPanel = { game: 1, assistant: 2, session: 5 };
    const panel = viewToPanel[view] ?? 1;
    switchMobilePanel(panel);
}

function showAllPanels() {
    if (window.innerWidth < 769) return;

    // Remove mobile-active and sub-panel-active classes for desktop
    document.querySelectorAll('.history-panel, .game-container, .info-panel').forEach(panel => {
        panel.classList.remove('mobile-active');
    });
    document.querySelectorAll('[data-mobile-panel]').forEach(el => {
        el.classList.remove('mobile-panel-active');
    });

    // Reset nav button states
    document.querySelectorAll('.mobile-nav-btn').forEach(btn => {
        btn.classList.remove('active');
    });
}

// ============================================================
// Admin Screen
// ============================================================

let adminUsers = [];       // cached user list
let roleModalUserId = null; // user being edited in role modal

function isCurrentUserAdmin() {
    return currentUser && currentUser.roles && currentUser.roles.includes('ROLE_ADMIN');
}

function loadAdminScreen() {
    const noAccess = document.getElementById('adminNoAccess');
    const content  = document.getElementById('adminContent');
    if (!isCurrentUserAdmin()) {
        if (noAccess) noAccess.style.display = '';
        if (content)  content.style.display = 'none';
        return;
    }
    if (noAccess) noAccess.style.display = 'none';
    if (content)  content.style.display = '';
    refreshAdminUsers();
}

async function refreshAdminUsers() {
    if (!isCurrentUserAdmin()) return;
    const tbody = document.getElementById('adminUserTableBody');
    if (!tbody) return;
    tbody.innerHTML = '<tr><td colspan="8" class="admin-table-empty">Loading users...</td></tr>';
    try {
        const resp = await fetch('/api/admin/users');
        if (!resp.ok) throw new Error('Failed to fetch users');
        const data = await resp.json();
        // Handle both paginated and non-paginated responses
        adminUsers = data.users || data.content || data;
        renderAdminUserTable();
        const subtitle = document.getElementById('adminSubtitle');
        if (subtitle) subtitle.textContent = `${adminUsers.length} user${adminUsers.length !== 1 ? 's' : ''} registered`;
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="8" class="admin-table-empty admin-error">Error: ${err.message}</td></tr>`;
    }
}

function renderAdminUserTable() {
    const tbody = document.getElementById('adminUserTableBody');
    if (!tbody) return;
    if (adminUsers.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="admin-table-empty">No users found.</td></tr>';
        return;
    }
    tbody.innerHTML = adminUsers.map(u => {
        const rolesHtml = (u.roles || []).map(r => {
            const shortRole = r.replace('ROLE_', '');
            const cls = shortRole === 'ADMIN' ? 'admin-badge-admin' : 'admin-badge-user';
            return `<span class="admin-badge ${cls}">${shortRole}</span>`;
        }).join(' ');
        const statusCls = u.enabled ? 'admin-status-active' : 'admin-status-disabled';
        const statusLabel = u.enabled ? 'Active' : 'Disabled';
        const isSelf = currentUser && currentUser.id === u.id;
        return `<tr>
            <td>${u.id}</td>
            <td>${escapeHtml(u.username || '\u2014')}</td>
            <td>${escapeHtml(u.email)}</td>
            <td>${escapeHtml(u.fullName || '\u2014')}</td>
            <td><span class="admin-badge admin-badge-provider">${escapeHtml(u.provider || 'local')}</span></td>
            <td>${rolesHtml}</td>
            <td><span class="admin-status ${statusCls}">${statusLabel}</span></td>
            <td class="admin-actions">
                <button class="btn btn-info btn-sm" onclick="openRoleModal(${u.id})" title="Manage roles">Roles</button>
                ${u.provider === 'local' ? `<button class="btn btn-secondary btn-sm" onclick="openPasswordModal(${u.id})" title="Reset password">Reset PW</button>` : ''}
                ${isSelf ? '' : (u.enabled
                    ? `<button class="btn btn-danger btn-sm" onclick="toggleUserEnabled(${u.id}, false)" title="Disable account">Disable</button>`
                    : `<button class="btn btn-success btn-sm" onclick="toggleUserEnabled(${u.id}, true)" title="Enable account">Enable</button>`)}
            </td>
        </tr>`;
    }).join('');
}

function escapeHtml(str) {
    if (!str) return '';
    const d = document.createElement('div');
    d.textContent = str;
    return d.innerHTML;
}

async function toggleUserEnabled(userId, enabled) {
    try {
        const endpoint = enabled ? 'enable' : 'disable';
        const resp = await fetch(`/api/admin/users/${userId}/${endpoint}`, {
            method: 'PUT'
        });
        if (!resp.ok) {
            const err = await resp.json();
            throw new Error(err.error || 'Request failed');
        }
        await refreshAdminUsers();
        showStatus(enabled ? 'User enabled' : 'User disabled', 'success');
    } catch (err) {
        showStatus('Error: ' + err.message, 'error');
    }
}

// ---- Role Modal ----

function openRoleModal(userId) {
    roleModalUserId = userId;
    const user = adminUsers.find(u => u.id === userId);
    if (!user) return;
    document.getElementById('roleModalTitle').textContent = `Roles \u2014 ${user.username || user.email}`;
    renderRoleModalRoles(user.roles || []);
    document.getElementById('roleModal').style.display = 'flex';
}

function closeRoleModal() {
    document.getElementById('roleModal').style.display = 'none';
    roleModalUserId = null;
}

function renderRoleModalRoles(roles) {
    const container = document.getElementById('roleModalCurrentRoles');
    if (!container) return;
    container.innerHTML = roles.map(r => {
        const shortRole = r.replace('ROLE_', '');
        const removable = shortRole !== 'USER';
        return `<span class="admin-badge admin-badge-lg ${shortRole === 'ADMIN' ? 'admin-badge-admin' : 'admin-badge-user'}">
            ${shortRole}
            ${removable ? `<button class="admin-badge-remove" onclick="removeRoleFromModal('${shortRole}')" title="Remove role">&times;</button>` : ''}
        </span>`;
    }).join(' ');
}

async function addSelectedRole() {
    const select = document.getElementById('roleModalSelect');
    const role = select.value;
    if (!role || !roleModalUserId) return;
    try {
        const resp = await fetch(`/api/admin/users/${roleModalUserId}/roles/${role}`, {
            method: 'POST'
        });
        if (!resp.ok) {
            const err = await resp.json();
            throw new Error(err.error || 'Request failed');
        }
        const updated = await resp.json();
        // Update cached user
        const idx = adminUsers.findIndex(u => u.id === roleModalUserId);
        if (idx >= 0) adminUsers[idx] = updated;
        renderRoleModalRoles(updated.roles || []);
        renderAdminUserTable();
        select.value = '';
        showStatus(`Role ${role} added`, 'success');
    } catch (err) {
        showStatus('Error: ' + err.message, 'error');
    }
}

async function removeRoleFromModal(shortRole) {
    if (!roleModalUserId) return;
    try {
        const resp = await fetch(`/api/admin/users/${roleModalUserId}/roles/${shortRole}`, {
            method: 'DELETE'
        });
        if (!resp.ok) {
            const err = await resp.json();
            throw new Error(err.error || 'Request failed');
        }
        const updated = await resp.json();
        const idx = adminUsers.findIndex(u => u.id === roleModalUserId);
        if (idx >= 0) adminUsers[idx] = updated;
        renderRoleModalRoles(updated.roles || []);
        renderAdminUserTable();
        showStatus(`Role ${shortRole} removed`, 'success');
    } catch (err) {
        showStatus('Error: ' + err.message, 'error');
    }
}

// ---- Password Reset Modal ----

let passwordModalUserId = null;

function openPasswordModal(userId) {
    passwordModalUserId = userId;
    const user = adminUsers.find(u => u.id === userId);
    if (!user) return;
    document.getElementById('passwordModalTitle').textContent = `Reset Password \u2014 ${user.username || user.email}`;
    document.getElementById('newPasswordInput').value = '';
    document.getElementById('confirmPasswordInput').value = '';
    document.getElementById('passwordModalError').style.display = 'none';
    document.getElementById('passwordModal').style.display = 'flex';
    document.getElementById('newPasswordInput').focus();
}

function closePasswordModal() {
    document.getElementById('passwordModal').style.display = 'none';
    passwordModalUserId = null;
}

async function submitPasswordReset() {
    const pw = document.getElementById('newPasswordInput').value;
    const confirm = document.getElementById('confirmPasswordInput').value;
    const errorDiv = document.getElementById('passwordModalError');

    if (!pw || pw.length < 8) {
        errorDiv.textContent = 'Password must be at least 8 characters.';
        errorDiv.style.display = 'block';
        return;
    }
    if (pw !== confirm) {
        errorDiv.textContent = 'Passwords do not match.';
        errorDiv.style.display = 'block';
        return;
    }
    errorDiv.style.display = 'none';

    try {
        const resp = await fetch(`/api/admin/users/${passwordModalUserId}/password`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ password: pw })
        });
        if (!resp.ok) {
            const err = await resp.json();
            throw new Error(err.error || 'Request failed');
        }
        closePasswordModal();
        showStatus('Password reset successfully', 'success');
    } catch (err) {
        errorDiv.textContent = err.message;
        errorDiv.style.display = 'block';
    }
}
