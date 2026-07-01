/**
 * Property Commerce Platform SDK — auth.js
 *
 * Token storage is namespaced 'pcp_*' (was 'ss_*' in the Shopify build) and
 * is no longer tied to a Shopify customer session — any host site works.
 *
 * Storage choice: localStorage for the access/refresh token pair so a
 * session survives page navigation across the host site (Shopify's
 * customer session did this implicitly; we now do it explicitly).
 */

const TOKEN_KEY = 'pcp_token';
const REFRESH_KEY = 'pcp_refresh';

export const Auth = {
  getToken() {
    return safeGet(TOKEN_KEY);
  },
  setToken(token) {
    safeSet(TOKEN_KEY, token);
  },
  getRefresh() {
    return safeGet(REFRESH_KEY);
  },
  setRefresh(token) {
    safeSet(REFRESH_KEY, token);
  },
  clear() {
    safeRemove(TOKEN_KEY);
    safeRemove(REFRESH_KEY);
  },
  isAuthenticated() {
    return !!this.getToken();
  },
  /**
   * Decodes the roles claim from the JWT payload (base64 middle segment).
   * No signature verification — the server already verified it.
   */
  getRoles() {
    const token = this.getToken();
    if (!token) return [];
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const roles = payload.roles || payload.authorities || [];
      return Array.isArray(roles) ? roles.map((r) => String(r).toLowerCase()) : [];
    } catch (_) {
      return [];
    }
  },
  getUserId() {
    const token = this.getToken();
    if (!token) return null;
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.sub || payload.userId || null;
    } catch (_) {
      return null;
    }
  },
  /** Authorization header object — merge into fetch headers. */
  headers() {
    const t = this.getToken();
    return t ? { Authorization: `Bearer ${t}` } : {};
  },
};

function safeGet(key) {
  try {
    return localStorage.getItem(key);
  } catch (_) {
    return null;
  }
}
function safeSet(key, value) {
  try {
    localStorage.setItem(key, value);
  } catch (_) {
    /* private browsing / storage disabled — auth simply won't persist */
  }
}
function safeRemove(key) {
  try {
    localStorage.removeItem(key);
  } catch (_) {}
}
