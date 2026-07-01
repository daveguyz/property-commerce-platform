/**
 * Property Commerce Platform SDK — auction-listing widget
 *
 * Port of the original plugin-auction.js (Shopify Liquid asset).
 * Key differences from the original:
 *   - No document.body.dataset reads — all config comes through `ctx`
 *     (api, i18n, config) injected by PCP.mount().
 *   - No reliance on pre-existing Liquid-rendered DOM (tabs, filter form,
 *     pagination nav) — the widget renders its own shell into the host's
 *     container element.
 *   - Styles are injected once via a <style> tag scoped with the
 *     pcp-auction-listing class prefix, since the host site has no
 *     base.css to inherit from.
 *   - URLs point to /auction-room?lot={id} relative paths are no longer
 *     assumed; an onSelectLot callback (or a configurable roomUrl
 *     template) lets the host decide where bidding happens.
 */

import { injectStylesOnce } from '../../core/src/style-inject.js';
import styles from './styles.js';

const TYPE_ICONS = { ENGLISH: '\u{1F4C8}', DUTCH: '\u{1F4C9}', REVERSE: '\u{1F504}', SEALED_BID: '\u{1F512}' };
const TYPE_LABELS = { ENGLISH: 'English', DUTCH: 'Dutch', REVERSE: 'Reverse', SEALED_BID: 'Sealed' };

export const AuctionListingWidget = {
  mount(el, props, ctx) {
    injectStylesOnce('pcp-auction-listing-styles', styles);

    const state = {
      statuses: props.statuses || ['OPEN', 'EXTENDED'],
      type: props.type || null,
      page: 0,
      totalPages: 0,
      timers: [],
      roomUrlTemplate: props.roomUrl || '#lot={lotId}', // host can override
    };

    el.className = (el.className ? el.className + ' ' : '') + 'pcp-auction-listing';
    el.innerHTML = renderShell();

    wireTabs(el, state, ctx);
    loadLots(el, state, ctx);

    // Cleanup: clear all countdown intervals when unmounted
    return () => {
      state.timers.forEach((id) => clearInterval(id));
    };
  },
};

function renderShell() {
  return `
    <div class="pcp-al__tabs" role="tablist" aria-label="Auction status">
      <button class="pcp-al__tab pcp-al__tab--active" data-statuses="OPEN,EXTENDED" role="tab" aria-selected="true">Live</button>
      <button class="pcp-al__tab" data-statuses="SCHEDULED" role="tab" aria-selected="false">Upcoming</button>
      <button class="pcp-al__tab" data-statuses="CLOSED,SETTLED,NO_RESERVE" role="tab" aria-selected="false">Closed</button>
    </div>
    <div class="pcp-al__grid" aria-live="polite"></div>
    <div class="pcp-al__pagination"></div>
  `;
}

function wireTabs(el, state, ctx) {
  el.querySelectorAll('.pcp-al__tab').forEach((tab) => {
    tab.addEventListener('click', () => {
      el.querySelectorAll('.pcp-al__tab').forEach((t) => {
        t.classList.remove('pcp-al__tab--active');
        t.setAttribute('aria-selected', 'false');
      });
      tab.classList.add('pcp-al__tab--active');
      tab.setAttribute('aria-selected', 'true');
      state.statuses = tab.dataset.statuses.split(',');
      state.page = 0;
      loadLots(el, state, ctx);
    });
  });
}

async function loadLots(el, state, ctx) {
  const grid = el.querySelector('.pcp-al__grid');
  grid.innerHTML = renderSkeletons();
  state.timers.forEach((id) => clearInterval(id));
  state.timers = [];

  try {
    const qs = new URLSearchParams({
      status: state.statuses.join(','),
      page: state.page,
      size: 12,
      ...(state.type ? { type: state.type } : {}),
    });
    const res = await ctx.api(`/api/v1/auctions?${qs}`);
    const lots = res?.data?.content || [];
    state.totalPages = res?.data?.totalPages || 1;

    if (!lots.length) {
      grid.innerHTML = `<p class="pcp-al__empty">No auctions found for this filter.</p>`;
      renderPagination(el, state, ctx);
      return;
    }

    grid.innerHTML = lots.map((lot) => renderCard(lot, ctx, state)).join('');
    bootstrapCountdowns(grid, state);
    renderPagination(el, state, ctx);
  } catch (e) {
    grid.innerHTML = `<p class="pcp-al__error">Could not load auctions. Please try again.</p>`;
  }
}

function renderSkeletons() {
  return Array.from({ length: 6 })
    .map(
      () => `
      <div class="pcp-al__card pcp-al__card--skeleton" aria-hidden="true">
        <div class="pcp-skel pcp-skel--img"></div>
        <div class="pcp-skel pcp-skel--line"></div>
        <div class="pcp-skel pcp-skel--line short"></div>
      </div>`
    )
    .join('');
}

function renderCard(lot, ctx, state) {
  const { escapeHtml: esc, i18n } = ctx;
  const isLive = lot.status === 'OPEN' || lot.status === 'EXTENDED';
  const isClosed = ['CLOSED', 'SETTLED', 'NO_RESERVE', 'CANCELLED'].includes(lot.status);
  const price = isLive && lot.currentBidAmount ? lot.currentBidAmount : lot.startingPrice;
  const displayPrice = isClosed && lot.winningAmount ? lot.winningAmount : price;
  const priceLabel = isLive ? 'Current bid' : isClosed ? 'Winning bid' : 'Starting';

  const roomUrl = state.roomUrlTemplate.replace('{lotId}', lot.id);

  const liveBadge = isLive
    ? `<span class="pcp-al__live-badge"><span class="pcp-al__live-dot"></span>Live</span>`
    : '';

  const countdown =
    isLive && lot.scheduledEndsAt
      ? `<div class="pcp-al__countdown" data-ends-at="${lot.scheduledEndsAt}" role="timer" aria-label="Time remaining">
           ${['d', 'h', 'm', 's']
             .map(
               (p, i) => `
             <span class="pcp-al__cd-seg"><b data-part="${p}">--</b><small>${['d', 'h', 'm', 's'][i]}</small></span>${i < 3 ? '<i>:</i>' : ''}`
             )
             .join('')}
         </div>`
      : lot.startsAt
        ? `<p class="pcp-al__timing">Starts ${i18n.fmtDate(lot.startsAt, { withTime: true })}</p>`
        : '';

  const ctaText = isLive ? 'Bid now' : isClosed ? 'View results' : 'View lot';
  const ctaClass = isLive ? 'pcp-btn--primary' : 'pcp-btn--ghost';

  return `
    <article class="pcp-al__card${isClosed ? ' pcp-al__card--closed' : ''}" data-lot-id="${esc(lot.id)}">
      <a href="${roomUrl}" class="pcp-al__img-link" tabindex="-1" aria-hidden="true">
        <div class="pcp-al__img-wrap">
          ${lot.firstImageUrl ? `<img src="${esc(lot.firstImageUrl)}" alt="${esc(lot.title)}" loading="lazy">` : `<div class="pcp-al__img-placeholder">\u{1F3E0}</div>`}
          <span class="pcp-al__type-badge">${TYPE_ICONS[lot.auctionType] || '\u{1F3F7}'} ${TYPE_LABELS[lot.auctionType] || lot.auctionType}</span>
          ${liveBadge}
        </div>
      </a>
      <div class="pcp-al__body">
        <p class="pcp-al__location">\u{1F4CD} ${esc(lot.location?.city || lot.propertyCity || '')}</p>
        <h3 class="pcp-al__title"><a href="${roomUrl}">${esc(lot.title)}</a></h3>
        ${countdown}
        <div class="pcp-al__price-row">
          <div>
            <span class="pcp-al__price-label">${priceLabel}</span>
            <p class="pcp-al__price">${i18n.fmt(displayPrice || 0)}</p>
          </div>
          <div class="pcp-al__bids">
            <span>${lot.totalBids || 0}</span><small>bids</small>
          </div>
        </div>
        <a href="${roomUrl}" class="pcp-btn ${ctaClass}">${ctaText}</a>
      </div>
    </article>`;
}

function bootstrapCountdowns(root, state) {
  root.querySelectorAll('.pcp-al__countdown[data-ends-at]').forEach((el) => {
    const end = new Date(el.dataset.endsAt).getTime();
    function tick() {
      const diff = end - Date.now();
      const set = (part, val) => {
        const seg = el.querySelector(`[data-part="${part}"]`);
        if (seg) seg.textContent = String(Math.max(0, val)).padStart(2, '0');
      };
      if (diff <= 0) {
        ['d', 'h', 'm', 's'].forEach((p) => set(p, 0));
        el.classList.add('pcp-al__countdown--expired');
        return;
      }
      set('d', Math.floor(diff / 86400000));
      set('h', Math.floor((diff % 86400000) / 3600000));
      set('m', Math.floor((diff % 3600000) / 60000));
      set('s', Math.floor((diff % 60000) / 1000));
      el.classList.toggle('pcp-al__countdown--urgent', diff < 300000);
    }
    tick();
    state.timers.push(setInterval(tick, 1000));
  });
}

function renderPagination(el, state, ctx) {
  const wrap = el.querySelector('.pcp-al__pagination');
  if (state.totalPages <= 1) {
    wrap.innerHTML = '';
    return;
  }
  wrap.innerHTML = `
    <button class="pcp-btn pcp-btn--ghost" ${state.page === 0 ? 'disabled' : ''} data-dir="-1">\u2190 Prev</button>
    <span>Page ${state.page + 1} of ${state.totalPages}</span>
    <button class="pcp-btn pcp-btn--ghost" ${state.page >= state.totalPages - 1 ? 'disabled' : ''} data-dir="1">Next \u2192</button>
  `;
  wrap.querySelectorAll('button[data-dir]').forEach((btn) => {
    btn.addEventListener('click', () => {
      state.page += parseInt(btn.dataset.dir, 10);
      loadLots(el, state, ctx);
    });
  });
}
