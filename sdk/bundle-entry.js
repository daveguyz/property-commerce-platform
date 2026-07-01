/**
 * Property Commerce Platform SDK — bundle-entry.js
 *
 * This is the file Vite actually builds (see vite.config.js). It imports
 * core (which sets window.PCP) and every widget package (which
 * self-register via registerWidget() on import), producing one file
 * a host site can drop in with a single <script> tag.
 *
 * As new widget packages are added (listing-widget, booking-widget,
 * account-widget — scaffolded, built out in subsequent phases), import
 * them here so they ship in the same CDN bundle.
 */
import PCP from './packages/core/src/index.js';
import '@property-commerce/auction-widget';

export default PCP;
