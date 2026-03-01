/**
 * Admin screen — user management (list, roles, enable/disable, password reset).
 * Imports: state, ui, api.
 */
import { state } from './state.js';
import { showStatus } from './ui.js';
import {
    apiGetAdminUsers, apiSetUserEnabled,
    apiAddUserRole, apiRemoveUserRole, apiResetPassword,
    apiGetActivityStats,
} from './api.js';

// ---- Access guard ----

function _isCurrentUserAdmin() {
    return state.currentUser && state.currentUser.roles &&
           state.currentUser.roles.includes('ROLE_ADMIN');
}

// ---- Main entry ----

export function loadAdminScreen() {
    const noAccess = document.getElementById('adminNoAccess');
    const content  = document.getElementById('adminContent');
    if (!_isCurrentUserAdmin()) {
        if (noAccess) noAccess.style.display = '';
        if (content)  content.style.display  = 'none';
        return;
    }
    if (noAccess) noAccess.style.display = 'none';
    if (content)  content.style.display  = '';
    switchAdminTab(state.adminTab || 'users');
}

export async function refreshAdminUsers() {
    if (!_isCurrentUserAdmin()) return;
    const tbody = document.getElementById('adminUserTableBody');
    if (!tbody) return;
    tbody.innerHTML = '<tr><td colspan="8" class="admin-table-empty">Loading users...</td></tr>';
    try {
        const resp = await apiGetAdminUsers();
        if (!resp.ok) throw new Error('Failed to fetch users');
        const data = await resp.json();
        state.adminUsers = data.users || data.content || data;
        _renderAdminUserTable();
        const subtitle = document.getElementById('adminSubtitle');
        if (subtitle) subtitle.textContent =
            `${state.adminUsers.length} user${state.adminUsers.length !== 1 ? 's' : ''} registered`;
    } catch (err) {
        tbody.innerHTML =
            `<tr><td colspan="8" class="admin-table-empty admin-error">Error: ${err.message}</td></tr>`;
    }
}

// ---- Table render ----

function _renderAdminUserTable() {
    const tbody = document.getElementById('adminUserTableBody');
    if (!tbody) return;
    if (state.adminUsers.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="admin-table-empty">No users found.</td></tr>';
        return;
    }
    tbody.innerHTML = state.adminUsers.map(u => {
        const rolesHtml = (u.roles || []).map(r => {
            const shortRole = r.replace('ROLE_', '');
            const cls = shortRole === 'ADMIN' ? 'admin-badge-admin' : 'admin-badge-user';
            return `<span class="admin-badge ${cls}">${shortRole}</span>`;
        }).join(' ');
        const statusCls   = u.enabled ? 'admin-status-active' : 'admin-status-disabled';
        const statusLabel = u.enabled ? 'Active' : 'Disabled';
        const isSelf      = state.currentUser && state.currentUser.id === u.id;
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

export function escapeHtml(str) {
    if (!str) return '';
    const d = document.createElement('div');
    d.textContent = str;
    return d.innerHTML;
}

// ---- Enable / Disable ----

export async function toggleUserEnabled(userId, enabled) {
    try {
        const resp = await apiSetUserEnabled(userId, enabled);
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

export function openRoleModal(userId) {
    state.roleModalUserId = userId;
    const user = state.adminUsers.find(u => u.id === userId);
    if (!user) return;
    document.getElementById('roleModalTitle').textContent =
        `Roles \u2014 ${user.username || user.email}`;
    _renderRoleModalRoles(user.roles || []);
    document.getElementById('roleModal').style.display = 'flex';
}

export function closeRoleModal() {
    document.getElementById('roleModal').style.display = 'none';
    state.roleModalUserId = null;
}

function _renderRoleModalRoles(roles) {
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

export async function addSelectedRole() {
    const select = document.getElementById('roleModalSelect');
    const role = select.value;
    if (!role || !state.roleModalUserId) return;
    try {
        const resp = await apiAddUserRole(state.roleModalUserId, role);
        if (!resp.ok) {
            const err = await resp.json();
            throw new Error(err.error || 'Request failed');
        }
        const updated = await resp.json();
        const idx = state.adminUsers.findIndex(u => u.id === state.roleModalUserId);
        if (idx >= 0) state.adminUsers[idx] = updated;
        _renderRoleModalRoles(updated.roles || []);
        _renderAdminUserTable();
        select.value = '';
        showStatus(`Role ${role} added`, 'success');
    } catch (err) {
        showStatus('Error: ' + err.message, 'error');
    }
}

export async function removeRoleFromModal(shortRole) {
    if (!state.roleModalUserId) return;
    try {
        const resp = await apiRemoveUserRole(state.roleModalUserId, shortRole);
        if (!resp.ok) {
            const err = await resp.json();
            throw new Error(err.error || 'Request failed');
        }
        const updated = await resp.json();
        const idx = state.adminUsers.findIndex(u => u.id === state.roleModalUserId);
        if (idx >= 0) state.adminUsers[idx] = updated;
        _renderRoleModalRoles(updated.roles || []);
        _renderAdminUserTable();
        showStatus(`Role ${shortRole} removed`, 'success');
    } catch (err) {
        showStatus('Error: ' + err.message, 'error');
    }
}

// ---- Password Reset Modal ----

export function openPasswordModal(userId) {
    state.passwordModalUserId = userId;
    const user = state.adminUsers.find(u => u.id === userId);
    if (!user) return;
    document.getElementById('passwordModalTitle').textContent =
        `Reset Password \u2014 ${user.username || user.email}`;
    document.getElementById('newPasswordInput').value     = '';
    document.getElementById('confirmPasswordInput').value = '';
    document.getElementById('passwordModalError').style.display = 'none';
    document.getElementById('passwordModal').style.display = 'flex';
    document.getElementById('newPasswordInput').focus();
}

export function closePasswordModal() {
    document.getElementById('passwordModal').style.display = 'none';
    state.passwordModalUserId = null;
}

export async function submitPasswordReset() {
    const pw      = document.getElementById('newPasswordInput').value;
    const confirm = document.getElementById('confirmPasswordInput').value;
    const errorDiv = document.getElementById('passwordModalError');

    if (!pw || pw.length < 8) {
        errorDiv.textContent     = 'Password must be at least 8 characters.';
        errorDiv.style.display   = 'block';
        return;
    }
    if (pw !== confirm) {
        errorDiv.textContent     = 'Passwords do not match.';
        errorDiv.style.display   = 'block';
        return;
    }
    errorDiv.style.display = 'none';

    try {
        const resp = await apiResetPassword(state.passwordModalUserId, pw);
        if (!resp.ok) {
            const err = await resp.json();
            throw new Error(err.error || 'Request failed');
        }
        closePasswordModal();
        showStatus('Password reset successfully', 'success');
    } catch (err) {
        errorDiv.textContent   = err.message;
        errorDiv.style.display = 'block';
    }
}

// ===========================================================
// Admin tabs
// ===========================================================

/**
 * Switch between the Users and Activity tabs in the admin screen.
 * @param {'users'|'activity'} tab
 */
export function switchAdminTab(tab) {
    state.adminTab = tab;

    const usersTab     = document.getElementById('adminTabUsers');
    const activityTab  = document.getElementById('adminTabActivity');
    const usersPanel   = document.getElementById('adminUsersPanel');
    const activityPanel = document.getElementById('adminActivityPanel');
    const subtitle     = document.getElementById('adminSubtitle');

    if (tab === 'activity') {
        if (usersTab)    usersTab.classList.remove('admin-tab-active');
        if (activityTab) activityTab.classList.add('admin-tab-active');
        if (usersPanel)   usersPanel.style.display   = 'none';
        if (activityPanel) activityPanel.style.display = '';
        if (subtitle) subtitle.textContent = 'Activity Log';
        refreshActivityStats();
    } else {
        if (usersTab)    usersTab.classList.add('admin-tab-active');
        if (activityTab) activityTab.classList.remove('admin-tab-active');
        if (usersPanel)   usersPanel.style.display   = '';
        if (activityPanel) activityPanel.style.display = 'none';
        refreshAdminUsers();
    }
}

/**
 * Refresh the activity stats panel — fetches from /api/wordai/admin/activity
 * and re-renders the table and summary cards.
 */
export async function refreshActivityStats() {
    if (!_isCurrentUserAdmin()) return;
    const tbody = document.getElementById('adminActivityTableBody');
    if (!tbody) return;
    tbody.innerHTML = '<tr><td colspan="8" class="admin-table-empty">Loading activity…</td></tr>';
    try {
        const resp = await apiGetActivityStats();
        if (!resp.ok) throw new Error('Failed to fetch activity stats');
        const data = await resp.json();
        state.adminActivityData = data.users || [];
        _renderActivitySummary(data);
        _renderActivityTable(data.users || []);
    } catch (err) {
        tbody.innerHTML =
            `<tr><td colspan="8" class="admin-table-empty admin-error">Error: ${err.message}</td></tr>`;
    }
}

// ---- Activity render helpers ----

function _renderActivitySummary(data) {
    const el = document.getElementById('adminActivitySummary');
    if (!el) return;
    const total     = data.totalUsers           || 0;
    const active7   = data.activeUsersLast7Days  || 0;
    const active30  = data.activeUsersLast30Days || 0;
    el.innerHTML = `
        <div class="admin-activity-stats">
            <div class="admin-activity-stat">
                <div class="admin-activity-stat-value">${total}</div>
                <div class="admin-activity-stat-label">Players</div>
            </div>
            <div class="admin-activity-stat">
                <div class="admin-activity-stat-value">${active7}</div>
                <div class="admin-activity-stat-label">Active (7d)</div>
            </div>
            <div class="admin-activity-stat">
                <div class="admin-activity-stat-value">${active30}</div>
                <div class="admin-activity-stat-label">Active (30d)</div>
            </div>
        </div>`;

    const subtitle = document.getElementById('adminSubtitle');
    if (subtitle) subtitle.textContent =
        `${total} players \u00b7 ${active7} active (7d) \u00b7 ${active30} active (30d)`;
}

function _renderActivityTable(users) {
    const tbody = document.getElementById('adminActivityTableBody');
    if (!tbody) return;
    if (users.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="admin-table-empty">No game activity recorded yet.</td></tr>';
        return;
    }
    tbody.innerHTML = users.map(u => {
        const winRate    = u.totalGames > 0
            ? Math.round((u.wonGames / u.totalGames) * 100) + '%'
            : '\u2014';
        const lastGame   = _formatDate(u.lastGameDate);
        const memberSince = _formatDate(u.firstGameDate);
        const cls7 = u.gamesLast7Days > 0 ? ' admin-activity-recent' : '';
        return `<tr>
            <td>${escapeHtml(u.username || '\u2014')}</td>
            <td>${escapeHtml(u.email || '\u2014')}</td>
            <td class="num">${u.totalGames}</td>
            <td class="num${cls7}">${u.gamesLast7Days}</td>
            <td class="num">${u.gamesLast30Days}</td>
            <td class="num">${winRate}</td>
            <td>${lastGame}</td>
            <td>${memberSince}</td>
        </tr>`;
    }).join('');
}

function _formatDate(dateStr) {
    if (!dateStr) return '\u2014';
    const d = new Date(dateStr);
    return d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
}
