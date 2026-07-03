/**
 * Property Commerce Platform — Tenant Dashboard (static SPA).
 *
 * Talks directly to the platform API using the tenant's API key
 * (X-Api-Key header — the ApiKeyGatewayFilter path from Phase C).
 * Credentials live in sessionStorage only; nothing is persisted
 * server-side by the dashboard itself.
 */
(function () {
  'use strict';

  const $ = (id) => document.getElementById(id);
  const state = {
    apiUrl:   sessionStorage.getItem('pcp_dash_api')    || '',
    tenantId: sessionStorage.getItem('pcp_dash_tenant') || '',
    apiKey:   sessionStorage.getItem('pcp_dash_key')    || '',
  };

  // ── API helper ─────────────────────────────────────────────────────
  async function api(path, options = {}) {
    const res = await fetch(state.apiUrl.replace(/\/$/, '') + path, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        'X-Api-Key': state.apiKey,
        'X-Tenant-Id': state.tenantId,
        ...(options.headers || {}),
      },
    });
    const body = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(body.message || 'HTTP ' + res.status);
    return body;
  }

  const esc = (s) => String(s ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  const fmtDate = (iso) => (iso ? new Date(iso).toLocaleString() : '–');

  // ── Router ─────────────────────────────────────────────────────────
  function show(page) {
    document.querySelectorAll('.page').forEach((p) => p.classList.add('hidden'));
    document.querySelectorAll('.nav-link').forEach((l) =>
      l.classList.toggle('active', l.dataset.page === page));
    ($('page-' + page) || $('page-connect')).classList.remove('hidden');
    if (page === 'overview')     loadOverview();
    if (page === 'api-keys')     loadKeys();
    if (page === 'webhooks')     loadWebhooks();
    if (page === 'integrations') renderIntegrations();
  }

  function route() {
    if (!state.apiUrl || !state.tenantId || !state.apiKey) { show('connect'); return; }
    show((location.hash || '#overview').slice(1));
  }
  window.addEventListener('hashchange', route);

  // ── Connect ────────────────────────────────────────────────────────
  $('conn-save').addEventListener('click', () => {
    state.apiUrl   = $('conn-api').value.trim();
    state.tenantId = $('conn-tenant').value.trim();
    state.apiKey   = $('conn-key').value.trim();
    sessionStorage.setItem('pcp_dash_api', state.apiUrl);
    sessionStorage.setItem('pcp_dash_tenant', state.tenantId);
    sessionStorage.setItem('pcp_dash_key', state.apiKey);
    location.hash = '#overview';
    route();
  });

  // ── Overview ───────────────────────────────────────────────────────
  async function loadOverview() {
    $('tenant-badge').textContent = state.tenantId;
    $('embed-snippet').textContent = [
      '<script>',
      '  window.PCPConfig = {',
      `    apiUrl: '${state.apiUrl}',`,
      `    tenantId: '${state.tenantId}',`,
      '  };',
      '<\/script>',
      '<script src="https://cdn.propertycommerce.io/sdk/v2/pcp.min.js"><\/script>',
      '<div id="listings"></div>',
      "<script>PCP.mount('auction-listing', '#listings');<\/script>",
    ].join('\n');
    try {
      const [tenant, keys, hooks] = await Promise.all([
        api(`/api/v1/tenants/${state.tenantId}/config`),
        api(`/api/v1/tenants/${state.tenantId}/api-keys`).catch(() => ({ data: [] })),
        api('/api/v1/webhooks').catch(() => ({ data: [] })),
      ]);
      $('stat-tenant').textContent   = tenant.data?.name || state.tenantId;
      $('stat-keys').textContent     = (keys.data || []).length;
      $('stat-webhooks').textContent = (hooks.data || []).length;
    } catch (e) {
      $('stat-tenant').textContent = 'connection failed';
    }
  }

  // ── API keys ───────────────────────────────────────────────────────
  async function loadKeys() {
    const tbody = $('keys-table').querySelector('tbody');
    tbody.innerHTML = '<tr><td colspan="5">Loading…</td></tr>';
    try {
      const res = await api(`/api/v1/tenants/${state.tenantId}/api-keys`);
      const keys = res.data || [];
      tbody.innerHTML = keys.length
        ? keys.map((k) => `
            <tr>
              <td>${esc(k.label)}</td>
              <td><code>${esc(k.keyPrefix)}</code></td>
              <td>${fmtDate(k.lastUsedAt)}</td>
              <td>${fmtDate(k.createdAt)}</td>
              <td><button class="btn-ghost btn-danger" data-revoke="${esc(k.id)}">Revoke</button></td>
            </tr>`).join('')
        : '<tr><td colspan="5">No keys yet.</td></tr>';
      tbody.querySelectorAll('[data-revoke]').forEach((btn) =>
        btn.addEventListener('click', async () => {
          if (!confirm('Revoke this key? Integrations using it will stop working immediately.')) return;
          await api(`/api/v1/tenants/${state.tenantId}/api-keys/${btn.dataset.revoke}`, { method: 'DELETE' });
          loadKeys();
        }));
    } catch (e) {
      tbody.innerHTML = `<tr><td colspan="5">Error: ${esc(e.message)}</td></tr>`;
    }
  }

  $('key-form').addEventListener('submit', async (ev) => {
    ev.preventDefault();
    try {
      const res = await api(`/api/v1/tenants/${state.tenantId}/api-keys`, {
        method: 'POST',
        body: JSON.stringify({ label: $('key-label').value, test: $('key-test').checked }),
      });
      const reveal = $('new-key-reveal');
      reveal.classList.remove('hidden');
      reveal.innerHTML = `⚠️ <strong>Copy this key now — it will never be shown again.</strong>
        <code>${esc(res.data.plaintextKey)}</code>`;
      $('key-label').value = '';
      loadKeys();
    } catch (e) { alert(e.message); }
  });

  // ── Webhooks ───────────────────────────────────────────────────────
  async function loadWebhooks() {
    const tbody = $('wh-table').querySelector('tbody');
    tbody.innerHTML = '<tr><td colspan="5">Loading…</td></tr>';
    try {
      const res = await api('/api/v1/webhooks');
      const hooks = res.data || [];
      tbody.innerHTML = hooks.length
        ? hooks.map((w) => `
            <tr>
              <td style="word-break:break-all">${esc(w.url)}</td>
              <td><code>${esc(w.eventFilter || 'all')}</code></td>
              <td>${w.failureCount}</td>
              <td>${fmtDate(w.lastDeliveryAt)}</td>
              <td>
                <button class="btn-ghost" data-log="${esc(w.id)}" data-url="${esc(w.url)}">Log</button>
                <button class="btn-ghost btn-danger" data-del="${esc(w.id)}">Delete</button>
              </td>
            </tr>`).join('')
        : '<tr><td colspan="5">No webhooks registered.</td></tr>';
      tbody.querySelectorAll('[data-del]').forEach((btn) =>
        btn.addEventListener('click', async () => {
          if (!confirm('Disable this endpoint?')) return;
          await api(`/api/v1/webhooks/${btn.dataset.del}`, { method: 'DELETE' });
          loadWebhooks();
        }));
      tbody.querySelectorAll('[data-log]').forEach((btn) =>
        btn.addEventListener('click', () => loadDeliveries(btn.dataset.log, btn.dataset.url)));
    } catch (e) {
      tbody.innerHTML = `<tr><td colspan="5">Error: ${esc(e.message)}</td></tr>`;
    }
  }

  async function loadDeliveries(endpointId, url) {
    const card = $('wh-deliveries-card');
    card.classList.remove('hidden');
    $('wh-deliveries-url').textContent = url;
    const tbody = $('wh-deliveries-table').querySelector('tbody');
    tbody.innerHTML = '<tr><td colspan="5">Loading…</td></tr>';
    try {
      const res = await api(`/api/v1/webhooks/${endpointId}/deliveries?size=20`);
      const items = res.data?.content || [];
      tbody.innerHTML = items.length
        ? items.map((d) => `
            <tr>
              <td>${esc(d.eventType)}</td>
              <td class="status-${esc(d.status)}">${esc(d.status)}</td>
              <td>${d.httpStatus ?? '–'}</td>
              <td>${d.attemptCount}</td>
              <td>${fmtDate(d.createdAt)}</td>
            </tr>`).join('')
        : '<tr><td colspan="5">No deliveries yet.</td></tr>';
    } catch (e) {
      tbody.innerHTML = `<tr><td colspan="5">Error: ${esc(e.message)}</td></tr>`;
    }
  }

  $('wh-form').addEventListener('submit', async (ev) => {
    ev.preventDefault();
    try {
      const res = await api('/api/v1/webhooks', {
        method: 'POST',
        body: JSON.stringify({ url: $('wh-url').value, eventFilter: $('wh-filter').value }),
      });
      const reveal = $('new-wh-reveal');
      reveal.classList.remove('hidden');
      reveal.innerHTML = `⚠️ <strong>Signing secret — store it now to verify deliveries:</strong>
        <code>${esc(res.data.signingSecret)}</code>`;
      $('wh-url').value = ''; $('wh-filter').value = '';
      loadWebhooks();
    } catch (e) { alert(e.message); }
  });

  // ── Integrations catalogue ─────────────────────────────────────────
  const INTEGRATIONS = [
    { name: 'Stripe',            cat: 'Payments',     live: true,  desc: 'Card payments, deposits, KYC identity verification.' },
    { name: 'PayFast',           cat: 'Payments',     live: false, desc: 'Southern-Africa payment gateway.' },
    { name: 'Peach Payments',    cat: 'Payments',     live: false, desc: 'African payment orchestration.' },
    { name: 'WeConvey',          cat: 'Conveyancing', live: false, desc: 'Automated conveyancing hand-off on agreement execution.' },
    { name: 'Salesforce',        cat: 'CRM',          live: true,  desc: 'Push bid + agreement events to Salesforce via webhooks.' },
    { name: 'HubSpot',           cat: 'CRM',          live: true,  desc: 'Contact + deal sync via webhooks.' },
    { name: 'Twilio SMS',        cat: 'Comms',        live: true,  desc: 'Outbid alerts and payment reminders by SMS.' },
    { name: 'WhatsApp Business', cat: 'Comms',        live: false, desc: 'Auction alerts on WhatsApp.' },
    { name: 'Google Analytics 4',cat: 'Analytics',    live: true,  desc: 'Bid + listing view events pushed to GA4.' },
    { name: 'WordPress',         cat: 'Platform',     live: true,  desc: 'Official plugin — shortcodes + Gutenberg blocks.' },
  ];

  function renderIntegrations() {
    $('int-grid').innerHTML = INTEGRATIONS.map((i) => `
      <div class="int-card">
        <span class="int-tag ${i.live ? 'live' : ''}">${i.live ? 'Available' : 'Coming soon'}</span>
        <h3>${esc(i.name)}</h3>
        <p>${esc(i.desc)}</p>
        <span class="hint">${esc(i.cat)}</span>
      </div>`).join('');
  }

  // ── Nav wiring + boot ──────────────────────────────────────────────
  document.querySelectorAll('.nav-link').forEach((l) =>
    l.addEventListener('click', () => setTimeout(route, 0)));
  route();
})();
