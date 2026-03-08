const BROWSER_SESSION_KEY = 'wordaiBrowserSessionId';

export function getBrowserSessionId() {
    let browserSessionId = sessionStorage.getItem(BROWSER_SESSION_KEY);
    if (browserSessionId) {
        return browserSessionId;
    }

    browserSessionId = typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
        ? crypto.randomUUID()
        : `wordai-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;

    sessionStorage.setItem(BROWSER_SESSION_KEY, browserSessionId);
    return browserSessionId;
}