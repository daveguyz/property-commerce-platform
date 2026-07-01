/**
 * Scoped styles for the auction-listing widget.
 * Reads --pcp-* custom properties set by core/config.js applyThemeTokens(),
 * so the widget automatically picks up the host's configured brand colour.
 */
export default `
.pcp-auction-listing {
  font-family: var(--pcp-font-family, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif);
  box-sizing: border-box;
}
.pcp-auction-listing *, .pcp-auction-listing *::before, .pcp-auction-listing *::after {
  box-sizing: inherit;
}

/* Tabs */
.pcp-al__tabs { display: flex; gap: 4px; margin-bottom: 20px; border-bottom: 1.5px solid #e5e7eb; }
.pcp-al__tab {
  padding: 8px 16px; border: none; background: none; cursor: pointer;
  font-size: .9375rem; font-weight: 600; color: #6b7280;
  border-bottom: 2.5px solid transparent; margin-bottom: -1.5px;
}
.pcp-al__tab--active { color: var(--pcp-primary-color, #6366f1); border-bottom-color: var(--pcp-primary-color, #6366f1); }

/* Grid */
.pcp-al__grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}
.pcp-al__empty, .pcp-al__error { color: #6b7280; padding: 40px 0; text-align: center; grid-column: 1/-1; }

/* Card */
.pcp-al__card {
  border: 1.5px solid #e5e7eb; border-radius: var(--pcp-border-radius, 8px);
  overflow: hidden; background: #fff; display: flex; flex-direction: column;
  transition: box-shadow .15s, border-color .15s;
}
.pcp-al__card:hover { border-color: var(--pcp-primary-color, #6366f1); box-shadow: 0 4px 20px rgba(0,0,0,.08); }
.pcp-al__card--closed { opacity: .75; }
.pcp-al__img-link { display: block; text-decoration: none; }
.pcp-al__img-wrap { position: relative; aspect-ratio: 3/2; background: #f3f4f6; overflow: hidden; }
.pcp-al__img-wrap img { width: 100%; height: 100%; object-fit: cover; display: block; }
.pcp-al__img-placeholder { display: flex; align-items: center; justify-content: center; height: 100%; font-size: 2rem; }
.pcp-al__type-badge {
  position: absolute; top: 10px; left: 10px; font-size: .75rem; font-weight: 700;
  padding: 3px 9px; border-radius: 20px; background: rgba(255,255,255,.92); color: #111;
}
.pcp-al__live-badge {
  position: absolute; top: 10px; right: 10px; font-size: .75rem; font-weight: 700;
  padding: 3px 9px; border-radius: 20px; background: #dcfce7; color: #15803d;
  display: flex; align-items: center; gap: 4px;
}
.pcp-al__live-dot { width: 6px; height: 6px; border-radius: 50%; background: #22c55e; animation: pcp-pulse 1.5s infinite; }
@keyframes pcp-pulse { 0%,100%{opacity:1} 50%{opacity:.4} }
@keyframes pcp-toast-in { from{opacity:0;transform:translateY(8px)} to{opacity:1;transform:none} }

.pcp-al__body { padding: 14px 16px; flex: 1; display: flex; flex-direction: column; gap: 6px; }
.pcp-al__location { font-size: .8125rem; color: #6b7280; margin: 0; }
.pcp-al__title { font-size: 1rem; font-weight: 700; margin: 0; line-height: 1.3; }
.pcp-al__title a { color: inherit; text-decoration: none; }

/* Countdown */
.pcp-al__countdown { display: flex; align-items: center; gap: 3px; font-variant-numeric: tabular-nums; margin: 4px 0; }
.pcp-al__cd-seg { display: flex; flex-direction: column; align-items: center; line-height: 1; }
.pcp-al__cd-seg b { font-size: .875rem; font-weight: 800; }
.pcp-al__cd-seg small { font-size: .5625rem; color: #9ca3af; text-transform: uppercase; }
.pcp-al__countdown i { font-style: normal; color: #9ca3af; margin: 0 1px; }
.pcp-al__countdown--urgent .pcp-al__cd-seg b { color: #ef4444; }
.pcp-al__timing { font-size: .8125rem; color: #6b7280; margin: 4px 0; }

.pcp-al__price-row { display: flex; align-items: flex-end; justify-content: space-between; margin-top: auto; padding-top: 8px; }
.pcp-al__price-label { font-size: .6875rem; color: #9ca3af; text-transform: uppercase; letter-spacing: .4px; display: block; }
.pcp-al__price { font-size: 1.125rem; font-weight: 800; color: var(--pcp-primary-color, #6366f1); margin: 2px 0 0; }
.pcp-al__bids { text-align: right; font-size: .8125rem; color: #6b7280; }
.pcp-al__bids span { display: block; font-weight: 700; font-size: .9375rem; color: #111; }

/* Buttons */
.pcp-btn {
  display: inline-block; padding: 9px 16px; border-radius: var(--pcp-border-radius, 8px);
  font-size: .875rem; font-weight: 700; text-decoration: none; text-align: center;
  margin-top: 10px; border: 1.5px solid transparent; cursor: pointer;
}
.pcp-btn--primary { background: var(--pcp-primary-color, #6366f1); color: var(--pcp-primary-text-color, #fff); }
.pcp-btn--ghost { background: none; border-color: #d1d5db; color: #374151; }
.pcp-btn:disabled { opacity: .4; cursor: not-allowed; }

/* Pagination */
.pcp-al__pagination { display: flex; align-items: center; justify-content: center; gap: 16px; margin-top: 24px; font-size: .875rem; }

/* Skeleton */
.pcp-skel { background: linear-gradient(90deg,#eee 25%,#f5f5f5 37%,#eee 63%); background-size: 400% 100%; animation: pcp-shimmer 1.4s ease infinite; border-radius: 4px; }
.pcp-skel--img { aspect-ratio: 3/2; }
.pcp-skel--line { height: 14px; margin: 10px 16px 0; }
.pcp-skel--line.short { width: 60%; }
@keyframes pcp-shimmer { 0%{background-position:100% 50%} 100%{background-position:0 50%} }
`;
