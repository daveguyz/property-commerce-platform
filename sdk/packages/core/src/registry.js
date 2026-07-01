/**
 * Property Commerce Platform SDK — registry.js
 *
 * Replaces the Shopify pattern of conditionally loading a specific
 * plugin-*.js file per page.handle in theme.liquid. Instead, every
 * widget self-registers here, and the host site calls one consistent
 * API regardless of which widgets are bundled in:
 *
 *   PCP.mount('auction-listing', '#my-div', { status: 'OPEN' });
 *   PCP.mount('auction-room', '#room', { lotId: 'abc-123' });
 *
 * A widget definition is: { mount(el, props, ctx) => cleanup? }
 * - el      the resolved DOM element (string selector or Element accepted)
 * - props   the options object passed to mount()
 * - ctx     shared SDK context: { api, auth, i18n, toast, escapeHtml }
 * - returns an optional cleanup function, called by PCP.unmount()
 */

const widgets = new Map();
const instances = new Map(); // selector -> { name, cleanup }

export function registerWidget(name, definition) {
  if (typeof definition?.mount !== 'function') {
    throw new Error(`PCP widget "${name}" must export a mount(el, props, ctx) function`);
  }
  widgets.set(name, definition);
}

export function mount(name, selector, props = {}, ctx) {
  const widget = widgets.get(name);
  if (!widget) {
    console.error(
      `[PCP] Unknown widget "${name}". Registered widgets: ${[...widgets.keys()].join(', ') || '(none loaded)'}`
    );
    return null;
  }

  const el = typeof selector === 'string' ? document.querySelector(selector) : selector;
  if (!el) {
    console.error(`[PCP] mount target not found: ${selector}`);
    return null;
  }

  // If something is already mounted on this element, clean it up first —
  // makes PCP.mount() idempotent for SPA-style host sites that re-render.
  unmountElement(el);

  const cleanup = widget.mount(el, props, ctx);
  const key = el;
  instances.set(key, { name, cleanup });
  el.dataset.pcpWidget = name;

  return {
    unmount: () => unmountElement(el),
  };
}

export function unmount(selector) {
  const el = typeof selector === 'string' ? document.querySelector(selector) : selector;
  if (el) unmountElement(el);
}

function unmountElement(el) {
  const instance = instances.get(el);
  if (instance) {
    if (typeof instance.cleanup === 'function') {
      try {
        instance.cleanup();
      } catch (e) {
        console.warn(`[PCP] Cleanup error for widget "${instance.name}":`, e);
      }
    }
    instances.delete(el);
    delete el.dataset.pcpWidget;
    el.innerHTML = '';
  }
}

export function listWidgets() {
  return [...widgets.keys()];
}
