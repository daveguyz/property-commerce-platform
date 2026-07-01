/**
 * Property Commerce Platform SDK — config.js
 *
 * Replaces the Shopify-specific pattern of reading <body data-*> attributes.
 * The host site now provides a single global object before the SDK script tag:
 *
 *   <script>
 *     window.PCPConfig = {
 *       apiUrl:   'https://api.propertycommerce.io',
 *       wsUrl:    'wss://api.propertycommerce.io/ws',
 *       tenantId: 'your-tenant-uuid',
 *       apiKey:   'pcp_live_xxx',   // optional — for server-rendered/no-login embeds
 *       currency: 'USD',
 *       locale:   'en',
 *       theme: { primaryColor: '#6366f1', fontFamily: 'inherit', borderRadius: '8px' },
 *     };
 *   </script>
 *
 * Every widget reads from this module instead of the DOM. This is the
 * single change that makes the SDK installable on any website rather
 * than only inside a Shopify Liquid theme.
 */

const DEFAULTS = {
  apiUrl: '',
  wsUrl: '',
  tenantId: null,
  apiKey: null,
  currency: 'USD',
  locale: 'en',
  theme: {
    primaryColor: '#6366f1',
    primaryTextColor: '#ffffff',
    fontFamily: 'inherit',
    borderRadius: '8px',
  },
  mockMode: false,
};

let CONFIG = { ...DEFAULTS };
let initialized = false;

/**
 * Reads window.PCPConfig (set by the host site) and merges with defaults.
 * Safe to call multiple times — later calls override earlier ones,
 * which lets PCP.init({...}) be used instead of the global object
 * for environments that prefer a function call (e.g. React apps).
 */
export function loadConfig(overrides = {}) {
  const fromWindow = (typeof window !== 'undefined' && window.PCPConfig) || {};
  CONFIG = {
    ...DEFAULTS,
    ...fromWindow,
    ...overrides,
    theme: {
      ...DEFAULTS.theme,
      ...(fromWindow.theme || {}),
      ...(overrides.theme || {}),
    },
  };

  // mockMode is implicit when no apiUrl is configured — lets the demo
  // site and widget playground work without a live backend.
  if (!CONFIG.apiUrl) CONFIG.mockMode = true;

  applyThemeTokens(CONFIG.theme);
  initialized = true;
  return CONFIG;
}

export function getConfig() {
  if (!initialized) loadConfig();
  return CONFIG;
}

/**
 * Applies the host's branding tokens as CSS custom properties on :root,
 * namespaced --pcp-* so they never collide with the host site's own
 * CSS variables. Widgets style themselves entirely from these tokens.
 */
function applyThemeTokens(theme) {
  if (typeof document === 'undefined') return;
  const root = document.documentElement;
  root.style.setProperty('--pcp-primary-color', theme.primaryColor);
  root.style.setProperty('--pcp-primary-text-color', theme.primaryTextColor);
  root.style.setProperty('--pcp-font-family', theme.fontFamily);
  root.style.setProperty('--pcp-border-radius', theme.borderRadius);
}

export function isMockMode() {
  return getConfig().mockMode === true;
}
