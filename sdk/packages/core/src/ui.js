/**
 * Property Commerce Platform SDK — ui.js
 *
 * Replaces the Shopify theme's reliance on pre-existing
 * #global-toast-container / #modal-backdrop elements in the Liquid
 * layout. Since the SDK can be embedded on any site, it creates its
 * own minimal containers on first use rather than requiring the host
 * to add specific markup.
 */

let toastContainer = null;

function ensureToastContainer() {
  if (toastContainer && document.body.contains(toastContainer)) return toastContainer;
  toastContainer = document.createElement('div');
  toastContainer.id = 'pcp-toast-container';
  toastContainer.setAttribute('aria-live', 'polite');
  toastContainer.style.cssText = [
    'position:fixed', 'bottom:20px', 'right:20px', 'z-index:99999',
    'display:flex', 'flex-direction:column', 'gap:8px',
    'font-family:var(--pcp-font-family, inherit)',
  ].join(';');
  document.body.appendChild(toastContainer);
  return toastContainer;
}

export function toast(message, type = 'default', duration = 4000) {
  if (typeof document === 'undefined') return;
  const container = ensureToastContainer();
  const el = document.createElement('div');
  el.setAttribute('role', 'status');
  el.textContent = message;

  const palette = {
    default: { bg: '#1f2937', fg: '#ffffff' },
    success: { bg: '#15803d', fg: '#ffffff' },
    error: { bg: '#b91c1c', fg: '#ffffff' },
    warning: { bg: '#92400e', fg: '#ffffff' },
  }[type] || { bg: '#1f2937', fg: '#ffffff' };

  el.style.cssText = [
    `background:${palette.bg}`, `color:${palette.fg}`,
    'padding:10px 16px', `border-radius:var(--pcp-border-radius, 8px)`,
    'font-size:14px', 'box-shadow:0 4px 16px rgba(0,0,0,.2)',
    'max-width:320px', 'animation:pcp-toast-in .2s ease',
  ].join(';');

  container.appendChild(el);
  setTimeout(() => el.remove(), duration);
}

export function escapeHtml(s) {
  return String(s || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}
