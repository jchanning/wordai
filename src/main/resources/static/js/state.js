/**
 * Shared mutable state for all game modules.
 * All module-level variables from the original game.js monolith live here.
 * Mutate properties directly: state.currentGameId = 'xyz';
 */
export const state = {
    API_BASE: '/api/wordai',

    // ---- Core game state ----
    currentGameId:       null,
    gameEnded:           false,
    currentWordLength:   5,
    currentDictionarySize: 2315,
    currentRemainingWords: 2315,
    helpUsedCount:       0,
    MAX_HELP_COUNT:      3,
    currentGameGuesses:  [],   // { attempt, guess, results, remainingWords, dictionaryMetrics }[]
    latestColumnLengths: null,
    latestOccurrenceData: null,
    letterStatusMap:     {},   // letter → 'G'|'A'|'R' across current game guesses

    // ---- Challenge mode ----
    challenge: {
        currentChallengeId:     null,
        dictionaryId:           null,
        status:                 'IDLE',
        totalScore:             0,
        totalPuzzles:           10,
        currentPuzzleNumber:    1,
        puzzlesCompleted:       0,
        currentPuzzleTimeLimitSeconds: 120,
        secondsRemaining:       120,
        currentPuzzleAssistsRemaining: 3,
        currentAttempts:        0,
        maxAttempts:            6,
        pauseUsed:              false,
        skipUsed:               false,
        challengeComplete:      false,
        challengeFailed:        false,
        message:                '',
        suggestedWord:          null,
        revealedTargetWord:     null,
        lastGuess:              null,
        completedPuzzles:       [],
        currentPuzzleGuesses:   [],
        keyboardStatusMap:      {},
        leaderboard:            [],
        selectedStrategy:       'ENTROPY',
        timerIntervalId:        null,
        countdownIntervalId:    null,
        countdownTimeoutId:     null,
        timerSyncedAtMs:        0,
        syncedSecondsRemaining: 120,
    },

    // ---- Auth ----
    currentUser: null,

    // ---- Dictionaries ----
    availableDictionaries: [],

    // ---- Navigation / view ----
    currentView:        'play',
    currentMobileView:  'game',   // legacy alias
    currentMobilePanel: 1,

    // ---- Status toast ----
    statusHideTimer: null,

    // ---- Dictionary screen ----
    dictionaryScreenState: {
        loading:      false,
        dictionaryId: null,
        words:        [],
        wordLength:   0,
        entropyMap:   {},
        _complexity:  null,   // stored after renderDictionaryComplexity
    },
    currentDictTab: 'frequency',

    letterFreqSortState: {
        column:    null,
        ascending: true,
    },

    dictionaryWordsState: {
        sortColumn:    1,
        sortAscending: false,
        searchTerm:    '',
        allWords:      [],
        wordLength:    0,
        entropyMap:    {},
    },

    // ---- Autoplay ----
    autoplayState: {
        isRunning:       false,
        shouldStop:      false,
        gameCount:       0,
        gamesCompleted:  0,
        strategy:        'RANDOM',
        sessionStats:    null,
    },

    // ---- Player analysis ----
    analysisState: {
        running:     false,
        shouldStop:  false,
        completed:   0,
        total:       0,
        summaryData: [],
        detailsData: [],
    },

    // ---- Admin ----
    adminUsers:         [],
    adminActivityData:  [],
    adminTab:           'users',
    roleModalUserId:    null,
    passwordModalUserId: null,
};
