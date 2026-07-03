# Property Commerce Platform — pcp-tools

Developer tooling for the Property Commerce Platform, in your editor.

- **Auction Monitor** — live lots with current bid, bid count, and countdown in the sidebar (auto-refreshes every 10s)
- **API Explorer** — browse every platform endpoint; click to insert a ready-to-run `fetch()` snippet with tenant headers
- **Snippets** — `pcp-init`, `pcp-auction-listing`, `pcp-auction-room`, `pcp-fetch`, `pcp-webhook-verify` for HTML, JS, and PHP (WordPress)

## Setup

Open Settings and set:
- `pcp.apiUrl` — your platform URL (default `https://api.propertycommerce.io`)
- `pcp.apiKey` — a tenant API key from the [dashboard](https://app.propertycommerce.io)
- `pcp.tenantId` — your tenant UUID
