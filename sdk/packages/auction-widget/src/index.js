/**
 * Property Commerce Platform SDK — @property-commerce/auction-widget
 *
 * Importing this module self-registers its widgets with the core
 * registry, so a host site only needs:
 *
 *   import '@property-commerce/core';
 *   import '@property-commerce/auction-widget';
 *   PCP.mount('auction-listing', '#listings');
 *
 * or, in the bundled CDN build, all widgets are pre-registered into
 * the single pcp.min.js file (see Phase G build config).
 */
import { registerWidget } from '../../core/src/registry.js';
import { AuctionListingWidget } from './listing.js';

registerWidget('auction-listing', AuctionListingWidget);

export { AuctionListingWidget };
