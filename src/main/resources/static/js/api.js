/**
 * Pure HTTP helper functions — one per API endpoint.
 * No DOM manipulation, no state imports.
 * Callers are responsible for handling errors and updating state/UI.
 */

const API_BASE = '/api/wordai';

async function apiFetch(url, options = {}) {
    const response = await fetch(url, {
        credentials: 'same-origin',
        ...options,
    });

    const responseUrl = response.url || '';
    const contentType = response.headers.get('content-type') || '';
    const isLoginRedirect = response.redirected && responseUrl.includes('/login.html');
    const isHtmlResponse = contentType.includes('text/html');

    if (isLoginRedirect || (url.startsWith('/api/') && isHtmlResponse)) {
        throw new Error('Your session has expired. Please sign in again.');
    }

    return response;
}

// ---- Auth ----

export async function apiCheckAuth() {
    return apiFetch('/api/auth/user');
}

// ---- Algorithms ----

export async function apiGetAlgorithms() {
    return apiFetch(`${API_BASE}/algorithms`);
}

// ---- Dictionaries ----

export async function apiGetDictionaries() {
    return apiFetch(`${API_BASE}/dictionaries`);
}

export async function apiGetDictionary(id) {
    return apiFetch(`${API_BASE}/dictionaries/${encodeURIComponent(id)}`);
}

// ---- Games ----

export async function apiCreateGame(body) {
    return apiFetch(`${API_BASE}/games`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
    });
}

export async function apiGetGameState(gameId) {
    return apiFetch(`${API_BASE}/games/${encodeURIComponent(gameId)}`);
}

export async function apiDeleteGame(gameId) {
    return apiFetch(`${API_BASE}/games/${encodeURIComponent(gameId)}`, {
        method: 'DELETE',
    });
}

export async function apiMakeGuess(gameId, word) {
    return apiFetch(`${API_BASE}/games/${encodeURIComponent(gameId)}/guess`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ word }),
    });
}

export async function apiGetSuggestion(gameId) {
    return apiFetch(`${API_BASE}/games/${encodeURIComponent(gameId)}/suggestion`);
}

export async function apiSetStrategy(gameId, strategy) {
    return apiFetch(`${API_BASE}/games/${encodeURIComponent(gameId)}/strategy`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ strategy }),
    });
}

// ---- Challenges ----

export async function apiCreateChallenge(body) {
    return apiFetch(`${API_BASE}/challenges`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body || {}),
    });
}

export async function apiGetChallengeState(challengeId) {
    return apiFetch(`${API_BASE}/challenges/${encodeURIComponent(challengeId)}`);
}

export async function apiMakeChallengeGuess(challengeId, word) {
    return apiFetch(`${API_BASE}/challenges/${encodeURIComponent(challengeId)}/guess`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ word }),
    });
}

export async function apiUseChallengeAssist(challengeId, strategy) {
    return apiFetch(`${API_BASE}/challenges/${encodeURIComponent(challengeId)}/assist`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ strategy }),
    });
}

export async function apiPauseChallenge(challengeId) {
    return apiFetch(`${API_BASE}/challenges/${encodeURIComponent(challengeId)}/pause`, {
        method: 'POST',
    });
}

export async function apiSkipChallenge(challengeId) {
    return apiFetch(`${API_BASE}/challenges/${encodeURIComponent(challengeId)}/skip`, {
        method: 'POST',
    });
}

export async function apiGetChallengeLeaderboard() {
    return apiFetch(`${API_BASE}/challenges/leaderboard`);
}

// ---- Health ----

export async function apiCheckHealth() {
    return apiFetch(`${API_BASE}/health`);
}

// ---- Admin ----

export async function apiGetAdminUsers() {
    return apiFetch('/api/admin/users');
}

export async function apiSetUserEnabled(userId, enabled) {
    return apiFetch(`/api/admin/users/${userId}/${enabled ? 'enable' : 'disable'}`, {
        method: 'PUT',
    });
}

export async function apiAddUserRole(userId, role) {
    return apiFetch(`/api/admin/users/${userId}/roles/${encodeURIComponent(role)}`, {
        method: 'POST',
    });
}

export async function apiRemoveUserRole(userId, role) {
    return apiFetch(`/api/admin/users/${userId}/roles/${encodeURIComponent(role)}`, {
        method: 'DELETE',
    });
}

export async function apiResetPassword(userId, password) {
    return apiFetch(`/api/admin/users/${userId}/password`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ password }),
    });
}

export async function apiGetActivityStats() {
    return apiFetch('/api/wordai/admin/activity');
}
