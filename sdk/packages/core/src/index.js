/**
 * Property Commerce Platform SDK — index.js
 * Core package entry point.
 *
 * Bundled (via the Vite build, Phase G) into pcp.min.js as an IIFE that
 * sets window.PCP. This is the only global the SDK introduces.
 *
 * Host site usage:
 *
 *   <script>
 *     window.PCPConfig = { apiUrl: '...', tenantId: '...' };
 *   </script>
 *   <script src="https://cdn.propertycommerce.io/sdk/v2/pcp.min.js"></script>
 *   <script>
 *     PCP.mount('auction-listing', '#listings');
 *   </script>
 *
 * Note: PCP.init() is called automatically on first import using
 * window.PCPConfig. Calling PCP.init({...}) manually is only needed
 * for apps that want to set config programmatically (e.g. a React app
 * that doesn't want a global script tag).
 */

import { loadConfig, getConfig } from './config.js';
import { Auth } from './auth.js';
import { api } from './api.js';
import { toast, escapeHtml } from './ui.js';
import { mount, unmount, registerWidget, listWidgets } from './registry.js';
import * as i18n from './i18n.js';

// Auto-init from window.PCPConfig as soon as the module loads.
loadConfig();

/** Shared context object passed to every widget's mount() function. */
function buildContext() {
  return { api, auth: Auth, i18n, toast, escapeHtml, config: getConfig() };
}

const PCP = {
  // ── Setup ──────────────────────────────────────────────────────────
  init(overrides) {
    return loadConfig(overrides);
  },
  config: getConfig,

  // ── Widgets ────────────────────────────────────────────────────────
  mount(name, selector, props) {
    return mount(name, selector, props, buildContext());
  },
  unmount,
  registerWidget,
  listWidgets,

  // ── Direct access for advanced/custom integrations ──────────────────
  api,
  auth: Auth,
  i18n,
  toast,

  version: '2.0.0',
};

if (typeof window !== 'undefined') {
  window.PCP = PCP;
  document.dispatchEvent(new CustomEvent('pcp:ready'));
}

export default PCP;
export { mount, unmount, registerWidget, api, Auth, toast, i18n };
