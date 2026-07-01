/**
 * Property Commerce Platform SDK — api.js
 *
 * The authenticated fetch wrapper every widget calls instead of raw fetch().
 *
 * Two auth modes, mirroring Phase C of the backend plan:
 *   1. Bearer JWT  — set after PCP login (Auth.getToken())
 *   2. X-API-Key   — set via PCPConfig.apiKey, for server-rendered embeds
 *      or contexts where no end-user login exists (read-only listing pages)
 *
 * Every request also carries X-Tenant-Id so the API gateway can route
 * and isolate data per tenant (Phase D backend — multi-tenancy).
 */

import { getConfig, isMockMode } from './config.js';
import { Auth } from './auth.js';
import { mockHandle } from './mock.js';

let refreshPromise = null;

export async function api(path, options = {}) {
  const config = getConfig();

  if (isMockMode()) {
    return mockHandle(path, options);
  }

  const url = config.apiUrl.replace(/\/$/, '') + path;
  const res = await fetch(url, {
    ...options,
    headers: buildHeaders(config, options),
  });

  if (res.status === 401) {
    const refreshed = await attemptTokenRefresh(config);
    if (refreshed) {
      // Retry once with the new token
      return api(path, options);
    }
    Auth.clear();
    emitAuthExpired();
  }

  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    const err = new Error(data.message || `API error ${res.status}`);
    err.status = res.status;
    err.data = data;
    throw err;
  }
  return data;
}

function buildHeaders(config, options) {
  const headers = {
    'Content-Type': 'application/json',
    ...(config.tenantId ? { 'X-Tenant-Id': config.tenantId } : {}),
    ...Auth.headers(),
    ...(options.headers || {}),
  };

  // API key is used only when there is no end-user JWT (e.g. anonymous
  // listing browsing on a host site with no login flow configured).
  if (!Auth.getToken() && config.apiKey) {
    headers['X-Api-Key'] = config.apiKey;
  }
  return headers;
}

async function attemptTokenRefresh(config) {
  const refresh = Auth.getRefresh();
  if (!refresh) return false;

  // De-duplicate concurrent refresh calls — multiple widgets may hit
  // a 401 at the same time (e.g. listing widget + account widget).
  if (!refreshPromise) {
    refreshPromise = fetch(config.apiUrl.replace(/\/$/, '') + '/api/v1/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: refresh }),
    })
      .then((r) => r.json())
      .then((data) => {
        if (data.success && data.data?.accessToken) {
          Auth.setToken(data.data.accessToken);
          Auth.setRefresh(data.data.refreshToken);
          return true;
        }
        return false;
      })
      .catch(() => false)
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

function emitAuthExpired() {
  if (typeof document === 'undefined') return;
  document.dispatchEvent(new CustomEvent('pcp:auth-expired'));
}
