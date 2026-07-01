/**
 * Property Commerce Platform SDK — style-inject.js
 *
 * Because the SDK runs on arbitrary host sites with no shared base.css,
 * each widget carries its own CSS as a template string and injects it
 * once into <head> on first mount. Idempotent — calling it from multiple
 * mounted instances of the same widget only inserts one <style> tag.
 *
 * All class names use the pcp- prefix to avoid collisions with the
 * host site's own CSS.
 */

const injected = new Set();

export function injectStylesOnce(id, cssText) {
  if (injected.has(id) || (typeof document !== 'undefined' && document.getElementById(id))) {
    injected.add(id);
    return;
  }
  if (typeof document === 'undefined') return;
  const style = document.createElement('style');
  style.id = id;
  style.textContent = cssText;
  document.head.appendChild(style);
  injected.add(id);
}
