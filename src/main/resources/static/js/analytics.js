/**
 * In-game analytics panel: word-count gauges, column-length chart,
 * occurrence table, most-frequent-char table, and the full dictionary
 * screen (letter-frequency, complexity, words tables).
 * Imports: state, ui.
 */
import { state } from './state.js';
import { showStatus } from './ui.js';

// ============================================================
// In-game analytics panel
// ============================================================

export function initializeAnalytics(dictionaryMetrics) {
    const totalWords = dictionaryMetrics.totalWords || 2315;
    state.currentDictionarySize = totalWords;

    document.getElementById('totalWords').textContent = totalWords;
    document.getElementById('remainingWords').textContent = totalWords;
    document.getElementById('eliminatedWords').textContent = '0';
    document.getElementById('reductionPercent').textContent = '0%';
    document.getElementById('uniqueLetters').textContent = dictionaryMetrics.uniqueCharacters || '26';
    document.getElementById('letterCount').textContent   = dictionaryMetrics.letterCount || '-';

    updateColumnLengthsChart(
        dictionaryMetrics.columnLengths && dictionaryMetrics.columnLengths.length > 0
            ? dictionaryMetrics.columnLengths : null
    );

    if (dictionaryMetrics.occurrenceCountByPosition) {
        state.latestOccurrenceData = dictionaryMetrics.occurrenceCountByPosition;
        updateOccurrenceTable(state.latestOccurrenceData);
    } else {
        state.latestOccurrenceData = null;
        updateOccurrenceTable(null);
    }

    if (dictionaryMetrics.mostFrequentCharByPosition) {
        updateMostFrequentTable(dictionaryMetrics.mostFrequentCharByPosition);
    } else {
        updateMostFrequentTable(null);
    }
}

export function resetAnalytics() {
    const totalWords = state.currentDictionarySize;
    document.getElementById('totalWords').textContent      = totalWords;
    document.getElementById('remainingWords').textContent  = totalWords;
    document.getElementById('eliminatedWords').textContent = '0';
    document.getElementById('reductionPercent').textContent = '0%';
    document.getElementById('uniqueLetters').textContent   = '26';
    document.getElementById('letterCount').textContent     = '-';
    updateColumnLengthsChart(null);
    updateOccurrenceTable(null);
    updateMostFrequentTable(null);
}

export function updateAnalytics(guessedWord, remainingCount, dictionaryMetrics) {
    const totalWords = state.currentDictionarySize;
    const eliminated = totalWords - remainingCount;
    const reductionPercent = ((eliminated / totalWords) * 100).toFixed(2);

    document.getElementById('remainingWords').textContent  = remainingCount;
    document.getElementById('eliminatedWords').textContent = eliminated;
    document.getElementById('reductionPercent').textContent = reductionPercent + '%';

    if (dictionaryMetrics) {
        document.getElementById('uniqueLetters').textContent = dictionaryMetrics.uniqueCharacters || '-';
        document.getElementById('letterCount').textContent   = dictionaryMetrics.letterCount || '-';

        if (dictionaryMetrics.columnLengths && dictionaryMetrics.columnLengths.length > 0) {
            updateColumnLengthsChart(dictionaryMetrics.columnLengths);
        } else {
            updateColumnLengthsChart(null);
        }

        if (dictionaryMetrics.occurrenceCountByPosition) {
            state.latestOccurrenceData = dictionaryMetrics.occurrenceCountByPosition;
            updateOccurrenceTable(state.latestOccurrenceData);
        }

        if (dictionaryMetrics.mostFrequentCharByPosition) {
            updateMostFrequentTable(dictionaryMetrics.mostFrequentCharByPosition);
        }
    }
}

export function updateColumnLengthsChart(columnLengths) {
    const chartContainer = document.getElementById('columnLengthsChart');
    if (!chartContainer) return;
    chartContainer.innerHTML = '';

    if (!columnLengths || columnLengths.length === 0) {
        chartContainer.innerHTML = '<div style="width:100%;text-align:center;color:#888;align-self:center;">No data yet</div>';
        return;
    }

    const maxHeight   = 26;
    const chartHeight = 120;

    columnLengths.forEach((count, index) => {
        const barContainer = document.createElement('div');
        barContainer.style.cssText = 'flex:1;display:flex;flex-direction:column;align-items:center;gap:4px;';

        const barWrapper = document.createElement('div');
        barWrapper.style.cssText = `height:${chartHeight}px;display:flex;align-items:flex-end;justify-content:center;width:100%;`;

        const heightPercent = (count / maxHeight) * 100;
        const barHeight = Math.min(chartHeight, (heightPercent / 100) * chartHeight);

        const bar = document.createElement('div');
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
        countLabel.textContent = count;
        barContainer.appendChild(countLabel);

        const positionLabel = document.createElement('div');
        positionLabel.className = 'pos-label';
        positionLabel.textContent = `P${index + 1}`;
        barContainer.appendChild(positionLabel);

        chartContainer.appendChild(barContainer);
    });
}

export function updateOccurrenceTable(occurrenceData) {
    const tableContainer = document.getElementById('occurrenceTable');
    if (!tableContainer) return;
    tableContainer.innerHTML = '';

    if (!occurrenceData || Object.keys(occurrenceData).length === 0) {
        tableContainer.innerHTML = '<div style="text-align:center;color:#888;padding:20px;">No data yet</div>';
        return;
    }

    const firstKey = Object.keys(occurrenceData)[0];
    const numPositions = occurrenceData[firstKey] ? occurrenceData[firstKey].length : 5;

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
    const letterStatusMap = state.letterStatusMap || {};

    allLetters.forEach((letter, idx) => {
        const row = document.createElement('tr');
        row.style.cssText = `border-bottom:1px solid rgba(255,255,255,0.1);${idx % 2 === 1 ? 'background:rgba(255,255,255,0.02);' : ''}`;

        const counts = occurrenceData[letter] || Array(numPositions).fill(0);
        const isEliminated = counts.every(count => count === 0);

        const letterCell = document.createElement('td');
        letterCell.textContent = letter.toUpperCase();
        letterCell.style.cssText = 'padding:6px 4px;font-weight:bold;text-align:center;';

        const status = letterStatusMap[letter.toUpperCase()];
        if (status === 'G')                  letterCell.classList.add('letter-status-correct');
        else if (status === 'A')             letterCell.classList.add('letter-status-present');
        else if (status === 'R' || status === 'X') letterCell.classList.add('letter-status-absent');
        else                                 letterCell.classList.add('letter-status-unused');

        if (isEliminated) {
            letterCell.style.opacity = '0.6';
            letterCell.style.textDecoration = 'line-through';
        }
        row.appendChild(letterCell);

        const maxCount = Math.max(...counts);
        for (let i = 0; i < numPositions; i++) {
            const countCell = document.createElement('td');
            const count = counts[i] || 0;
            countCell.textContent = count;

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

export function updateMostFrequentTable(mostFrequentData) {
    const tableContainer = document.getElementById('mostFrequentTable');
    if (!tableContainer) return;
    tableContainer.innerHTML = '';

    if (!mostFrequentData || mostFrequentData.length === 0) {
        tableContainer.innerHTML = '<div style="text-align:center;color:#888;padding:20px;">No data yet</div>';
        return;
    }

    const table = document.createElement('table');
    table.style.cssText = 'width:100%;border-collapse:collapse;font-size:0.9em;font-family:monospace;';

    const row = document.createElement('tr');
    mostFrequentData.forEach(letter => {
        const cell = document.createElement('td');
        cell.textContent = letter ? letter.toUpperCase() : '-';
        cell.style.cssText = `padding:12px 8px;text-align:center;font-weight:700;font-size:1em;color:var(--correct-color);border:1px solid rgba(255,255,255,0.1);background:rgba(106,170,100,0.15);`;
        row.appendChild(cell);
    });
    table.appendChild(row);

    const labelRow = document.createElement('tr');
    mostFrequentData.forEach((letter, index) => {
        const labelCell = document.createElement('td');
        labelCell.textContent = `P${index + 1}`;
        labelCell.className = 'pos-label';
        labelCell.style.cssText = 'padding:6px 4px;text-align:center;border:1px solid rgba(255,255,255,0.1);';
        labelRow.appendChild(labelCell);
    });
    table.appendChild(labelRow);
    tableContainer.appendChild(table);
}

// ============================================================
// Dictionary screen functions
// ============================================================

export function setDictionaryScreenPlaceholders(message) {
    const nameEl   = document.getElementById('dictionaryName');
    const lengthEl = document.getElementById('dictionaryWordLength');
    const countEl  = document.getElementById('dictionaryWordCount');
    const distEl   = document.getElementById('dictionaryComplexityDistribution');
    const freqEl   = document.getElementById('dictionaryLetterFrequency');
    const wordsEl  = document.getElementById('dictionaryWords');

    if (nameEl)   nameEl.textContent   = message || 'Select a dictionary from the menu above.';
    if (lengthEl) lengthEl.textContent = '-';
    if (countEl)  countEl.textContent  = '-';

    const empty = '<p class="muted-center muted-small">No data loaded.</p>';
    if (distEl)  distEl.innerHTML  = empty;
    if (freqEl)  freqEl.innerHTML  = empty;
    if (wordsEl) wordsEl.innerHTML = '<p class="muted-center muted-small">No dictionary selected.</p>';
}

export function refreshDictionaryScreen() {
    const selector = document.getElementById('dictionarySelector');
    const ds = state.dictionaryScreenState;
    if (!selector || !state.availableDictionaries || state.availableDictionaries.length === 0) {
        setDictionaryScreenPlaceholders('Loading dictionaries\u2026');
        return;
    }

    const dictionaryId = selector.value;
    const selectedDict = state.availableDictionaries.find(d => d.id === dictionaryId);
    if (!dictionaryId || !selectedDict || !selectedDict.available) {
        setDictionaryScreenPlaceholders('Select a dictionary from the menu above.');
        return;
    }

    if (ds.loading && ds.dictionaryId === dictionaryId) return;

    ds.loading = true;
    ds.dictionaryId = dictionaryId;
    setDictionaryScreenPlaceholders(`Loading ${selectedDict.name}\u2026`);

    loadDictionaryScreenData(dictionaryId, selectedDict.name)
        .catch(err => {
            console.error('Dictionary screen load failed:', err);
            setDictionaryScreenPlaceholders('Failed to load dictionary.');
            showStatus('Failed to load dictionary: ' + err.message, 'error');
        })
        .finally(() => { ds.loading = false; });
}

export async function loadDictionaryScreenData(dictionaryId, dictionaryName) {
    const response = await fetch(`/api/wordai/dictionaries/${encodeURIComponent(dictionaryId)}`);
    if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);

    const data = await response.json();
    const words = Array.isArray(data.words) ? data.words : [];
    const wordLength = data.wordLength || (words[0] ? words[0].length : 0);
    const entropyMap = data.entropy || {};

    const nameEl   = document.getElementById('dictionaryName');
    const lengthEl = document.getElementById('dictionaryWordLength');
    const countEl  = document.getElementById('dictionaryWordCount');
    if (nameEl)   nameEl.textContent   = dictionaryName || data.id || dictionaryId;
    if (lengthEl) lengthEl.textContent = String(wordLength || '-');
    if (countEl)  countEl.textContent  = String(words.length || 0);

    renderDictionaryLetterFrequency(words, wordLength);
    renderDictionaryComplexity(words, wordLength);
    renderDictionaryWords(words, wordLength, entropyMap);

    if (window.innerWidth <= 768) {
        switchDictTab(state.currentDictTab);
    }
}

// State for letter-frequency table sorting (module-level — mirrors original const)
export function sortLetterFrequency(columnIndex) {
    const lfs = state.letterFreqSortState;
    if (lfs.column === columnIndex) {
        lfs.ascending = !lfs.ascending;
    } else {
        lfs.column    = columnIndex;
        lfs.ascending = columnIndex === 0;
    }
    refreshDictionaryScreen();
}

export function renderDictionaryLetterFrequency(words, wordLength) {
    const container = document.getElementById('dictionaryLetterFrequency');
    if (!container) return;

    if (!words || words.length === 0 || !wordLength) {
        container.innerHTML = '<p class="muted-center muted-small">No data loaded.</p>';
        return;
    }

    const countsByLetter = new Map();
    for (const word of words) {
        for (let i = 0; i < wordLength; i++) {
            const letter = (word[i] || '').toUpperCase();
            if (!letter) continue;
            if (!countsByLetter.has(letter)) countsByLetter.set(letter, new Array(wordLength).fill(0));
            countsByLetter.get(letter)[i] += 1;
        }
    }

    let letters = Array.from(countsByLetter.keys()).sort();
    const lfs = state.letterFreqSortState;
    if (lfs.column !== null) {
        letters = _sortLetterFrequencyData(letters, countsByLetter, wordLength, lfs.column, lfs.ascending);
    }

    const maxByPos = new Array(wordLength).fill(0);
    for (const letter of letters) {
        const counts = countsByLetter.get(letter);
        for (let i = 0; i < wordLength; i++) {
            if (counts[i] > maxByPos[i]) maxByPos[i] = counts[i];
        }
    }

    const headerCells = ['Letter'];
    for (let i = 0; i < wordLength; i++) headerCells.push(`Pos ${i + 1}`);
    headerCells.push('Total');

    let html = '<table class="analysis-table"><thead><tr>';
    headerCells.forEach((h, idx) => {
        const sortClass = lfs.column === idx ? (lfs.ascending ? 'sort-asc' : 'sort-desc') : '';
        const sortIndicator = lfs.column === idx ? (lfs.ascending ? ' \u25b2' : ' \u25bc') : '';
        html += `<th class="sortable ${sortClass}" onclick="sortLetterFrequency(${idx})" style="cursor:pointer;" title="Click to sort">${h}${sortIndicator}</th>`;
    });
    html += '</tr></thead><tbody>';

    for (const letter of letters) {
        const counts = countsByLetter.get(letter);
        const total = counts.reduce((sum, n) => sum + n, 0);
        html += `<tr><td class="mono">${letter}</td>`;
        for (let i = 0; i < wordLength; i++) {
            const value = counts[i] || 0;
            const max   = maxByPos[i] || 0;
            const ratio = max > 0 ? (value / max) : 0;
            const heat  = Math.round(ratio * 90);
            html += `<td class="num heat-cell" style="--heat:${heat}%;" title="${value} occurrences at position ${i + 1}">${value}</td>`;
        }
        html += `<td class="num">${total}</td></tr>`;
    }

    html += '</tbody></table>';
    container.innerHTML = html;
}

function _sortLetterFrequencyData(letters, countsByLetter, wordLength, column, ascending) {
    const sorted = [...letters];
    sorted.sort((a, b) => {
        if (column === 0) {
            const cmp = a.localeCompare(b);
            return ascending ? cmp : -cmp;
        } else if (column === wordLength + 1) {
            const valA = countsByLetter.get(a).reduce((s, n) => s + n, 0);
            const valB = countsByLetter.get(b).reduce((s, n) => s + n, 0);
            return ascending ? valA - valB : valB - valA;
        } else {
            const posIndex = column - 1;
            const valA = countsByLetter.get(a)[posIndex] || 0;
            const valB = countsByLetter.get(b)[posIndex] || 0;
            return ascending ? valA - valB : valB - valA;
        }
    });
    return sorted;
}

// ---- Complexity chart ----

function _computeOneAwayNeighbourCounts(words, wordLength) {
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

function _getComplexityBucket(score) {
    if (score <= 2)  return { key: 'very-easy', label: 'Very Easy',  range: '0-2'   };
    if (score <= 5)  return { key: 'easy',      label: 'Easy',       range: '3-5'   };
    if (score <= 8)  return { key: 'medium',    label: 'Medium',     range: '6-8'   };
    if (score <= 11) return { key: 'hard',      label: 'Hard',       range: '9-11'  };
    return               { key: 'very-hard', label: 'Very Hard', range: '12-15' };
}

export function renderDictionaryComplexity(words, wordLength) {
    const distEl = document.getElementById('dictionaryComplexityDistribution');
    if (!distEl) return;
    if (!words || words.length === 0 || !wordLength) {
        distEl.innerHTML = '<p class="muted-center muted-small">No data loaded.</p>';
        return;
    }

    const neighbourCounts = _computeOneAwayNeighbourCounts(words, wordLength);
    const counts = { 'very-easy': 0, 'easy': 0, 'medium': 0, 'hard': 0, 'very-hard': 0 };
    for (const w of words) {
        counts[_getComplexityBucket(neighbourCounts.get(w) || 0).key]++;
    }

    const total = words.length;
    const pct = (n) => (total > 0 ? Math.round((n / total) * 100) : 0);
    const maxCount = Math.max(...Object.values(counts));
    const chartHeight = 140;

    const levels = [
        { key: 'very-easy', label: 'Very Easy', range: '0-2',   color: '#22c55e' },
        { key: 'easy',      label: 'Easy',      range: '3-5',   color: '#84cc16' },
        { key: 'medium',    label: 'Medium',    range: '6-8',   color: '#eab308' },
        { key: 'hard',      label: 'Hard',      range: '9-11',  color: '#f97316' },
        { key: 'very-hard', label: 'Very Hard', range: '12-15', color: '#ef4444' },
    ];

    let chartHTML = '<div class="complexity-bar-chart">';
    for (const level of levels) {
        const count = counts[level.key];
        const barHeight = maxCount > 0 ? (count / maxCount) * chartHeight : 0;
        chartHTML += `
            <div class="complexity-bar-container">
                <div class="complexity-bar-wrapper" style="height:${chartHeight}px;">
                    <div class="complexity-bar" style="height:${barHeight}px;background:linear-gradient(180deg,${level.color} 0%,${level.color}dd 100%);" title="${level.label}: ${count} words (${pct(count)}%)"></div>
                </div>
                <div class="complexity-bar-count">${count}</div>
                <div class="complexity-bar-percent">${pct(count)}%</div>
                <div class="complexity-bar-label">${level.label}</div>
                <div class="complexity-bar-range">${level.range}</div>
            </div>`;
    }
    chartHTML += '</div>';
    distEl.innerHTML = chartHTML;

    state.dictionaryScreenState._complexity = { neighbourCounts };
}

// ---- Dictionary words table ----

export function renderDictionaryWords(words, wordLength, entropyMap = {}) {
    const container = document.getElementById('dictionaryWords');
    if (!container) return;
    if (!words || words.length === 0) {
        container.innerHTML = '<p class="muted-center muted-small">No dictionary selected.</p>';
        return;
    }

    const dws = state.dictionaryWordsState;
    dws.allWords   = words;
    dws.wordLength = wordLength;
    dws.entropyMap = entropyMap;

    const complexity = state.dictionaryScreenState._complexity;
    const neighbourCounts = complexity?.neighbourCounts || new Map();

    let wordInfos = words.map(w => {
        const score   = neighbourCounts.get(w) || 0;
        const bucket  = _getComplexityBucket(score);
        const entropy = entropyMap[w] || 0;
        return { word: w, score, bucket, entropy };
    });

    const searchTerm = dws.searchTerm.toLowerCase().trim();
    if (searchTerm) {
        wordInfos = wordInfos.filter(info => info.word.toLowerCase().includes(searchTerm));
    }

    wordInfos = _sortDictionaryWordsData(wordInfos, dws.sortColumn, dws.sortAscending);

    const headers = ['Word', '1-away', 'Complexity', 'Entropy'];
    let html = '<table class="analysis-table"><thead><tr>';
    headers.forEach((h, idx) => {
        const sortClass     = dws.sortColumn === idx ? (dws.sortAscending ? 'sort-asc' : 'sort-desc') : '';
        const sortIndicator = dws.sortColumn === idx ? (dws.sortAscending ? ' \u25b2' : ' \u25bc') : '';
        const thClass = (idx === 1 || idx === 3) ? 'num' : '';
        html += `<th class="sortable ${sortClass} ${thClass}" onclick="sortDictionaryWords(${idx})" style="cursor:pointer;" title="Click to sort">${h}${sortIndicator}</th>`;
    });
    html += '</tr></thead><tbody>';

    if (wordInfos.length === 0) {
        html += '<tr><td colspan="4" class="muted-center" style="padding:20px;">No words match your search</td></tr>';
    } else {
        for (const info of wordInfos) {
            html += `
                <tr>
                    <td class="mono">${info.word.toUpperCase()}</td>
                    <td class="num">${info.score}</td>
                    <td><span class="complexity-pill complexity-${info.bucket.key}">${info.bucket.label}</span></td>
                    <td class="num">${info.entropy.toFixed(3)}</td>
                </tr>`;
        }
    }
    html += '</tbody></table>';
    container.innerHTML = html;

    matchDictionaryColumnHeights();
}

export function sortDictionaryWords(columnIndex) {
    const dws = state.dictionaryWordsState;
    if (dws.sortColumn === columnIndex) {
        dws.sortAscending = !dws.sortAscending;
    } else {
        dws.sortColumn    = columnIndex;
        dws.sortAscending = columnIndex === 0;
    }
    renderDictionaryWords(dws.allWords, dws.wordLength, dws.entropyMap);
}

export function filterDictionaryWords() {
    const searchInput = document.getElementById('dictionaryWordsSearch');
    if (searchInput) {
        state.dictionaryWordsState.searchTerm = searchInput.value;
        const dws = state.dictionaryWordsState;
        renderDictionaryWords(dws.allWords, dws.wordLength, dws.entropyMap);
    }
}

function _sortDictionaryWordsData(wordInfos, column, ascending) {
    const sorted = [...wordInfos];
    sorted.sort((a, b) => {
        if (column === 0) {
            const cmp = a.word.localeCompare(b.word);
            return ascending ? cmp : -cmp;
        }
        const complexityOrder = { 'very-easy': 0, 'easy': 1, 'medium': 2, 'hard': 3, 'very-hard': 4 };
        const valA = column === 1 ? a.score : (column === 2 ? complexityOrder[a.bucket.key] || 0 : a.entropy || 0);
        const valB = column === 1 ? b.score : (column === 2 ? complexityOrder[b.bucket.key] || 0 : b.entropy || 0);
        return ascending ? valA - valB : valB - valA;
    });
    return sorted;
}

export function matchDictionaryColumnHeights() {
    if (window.innerWidth <= 768) return;
    const leftPanel = document.querySelector('.dictionary-col-left .dictionary-panel');
    const rightCol  = document.querySelector('.dictionary-col-right');
    if (!leftPanel || !rightCol) return;
    rightCol.style.height = '';
    rightCol.style.height = `${leftPanel.offsetHeight}px`;
}

// ---- Dict tab switcher (mobile) ----

export function switchDictTab(tab) {
    if (window.innerWidth > 768) return;
    state.currentDictTab = tab;
    const leftCol        = document.querySelector('#screen-dictionary .dictionary-col-left');
    const rightCol       = document.querySelector('#screen-dictionary .dictionary-col-right');
    const complexityPanel = document.getElementById('dict-panel-complexity');
    const wordsPanel      = document.getElementById('dict-panel-words');
    if (!leftCol || !rightCol) return;

    if (tab === 'frequency') {
        leftCol.style.display  = 'flex';
        rightCol.style.display = 'none';
    } else if (tab === 'complexity') {
        leftCol.style.display  = 'none';
        rightCol.style.display = 'flex';
        if (complexityPanel) complexityPanel.style.display = 'block';
        if (wordsPanel)      wordsPanel.style.display      = 'none';
    } else if (tab === 'words') {
        leftCol.style.display  = 'none';
        rightCol.style.display = 'flex';
        if (complexityPanel) complexityPanel.style.display = 'none';
        if (wordsPanel)      wordsPanel.style.display      = 'flex';
    }

    document.querySelectorAll('#dictBottomNav .mobile-nav-btn').forEach(btn => {
        btn.classList.toggle('active', btn.getAttribute('data-tab') === tab);
    });
}
