/**
 * Pure HTTP helper functions — one per API endpoint.
 * No DOM manipulation, no state imports.
 * Callers are responsible for handling errors and updating state/UI.
 */

const API_BASE = '/api/wordai';

// ---- Auth ----

export async function apiCheckAuth() {
    return fetch('/api/auth/user');
}

// ---- Algorithms ----

export async function apiGetAlgorithms() {
    return fetch(`${API_BASE}/algorithms`);
}

// ---- Dictionaries ----

export async function apiGetDictionaries() {
    return fetch(`${API_BASE}/dictionaries`);
}

export async function apiGetDictionary(id) {
    return fetch(`${API_BASE}/dictionaries/${encodeURIComponent(id)}`);
}

// ---- Games ----

export async function apiCreateGame(body) {
    return fetch(`${API_BASE}/games`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
    });
}

export async function apiGetGameState(gameId) {
    return fetch(`${API_BASE}/games/${encodeURIComponent(gameId)}`);
}

export async function apiDeleteGame(gameId) {
    return fetch(`${API_BASE}/games/${encodeURIComponent(gameId)}`, {
        method: 'DELETE',
    });
}

export async function apiMakeGuess(gameId, word) {
    return fetch(`${API_BASE}/games/${encodeURIComponent(gameId)}/guess`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ word }),
    });
}

export async function apiGetSuggestion(gameId) {
    return fetch(`${API_BASE}/games/${encodeURIComponent(gameId)}/suggestion`);
}

export async function apiSetStrategy(gameId, strategy) {
    return fetch(`${API_BASE}/games/${encodeURIComponent(gameId)}/strategy`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ strategy }),
    });
}

// ---- Health ----

export async function apiCheckHealth() {
    return fetch(`${API_BASE}/health`);
}

// ---- Admin ----

export async function apiGetAdminUsers() {
    return fetch('/api/admin/users');
}

export async function apiSetUserEnabled(userId, enabled) {
    return fetch(`/api/admin/users/${userId}/${enabled ? 'enable' : 'disable'}`, {
        method: 'PUT',
    });
}

export async function apiAddUserRole(userId, role) {
    return fetch(`/api/admin/users/${userId}/roles/${encodeURIComponent(role)}`, {
        method: 'POST',
    });
}

export async function apiRemoveUserRole(userId, role) {
    return fetch(`/api/admin/users/${userId}/roles/${encodeURIComponent(role)}`, {
        method: 'DELETE',
    });
}

export async function apiResetPassword(userId, password) {
    return fetch(`/api/admin/users/${userId}/password`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ password }),
    });
}
