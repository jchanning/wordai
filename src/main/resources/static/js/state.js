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
    helpUsedCount:       0,
    MAX_HELP_COUNT:      3,
    currentGameGuesses:  [],   // { attempt, guess, results, remainingWords, dictionaryMetrics }[]
    latestOccurrenceData: null,
    letterStatusMap:     {},   // letter → 'G'|'A'|'R' across current game guesses

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
