# Quickstart — Add an auction tab to your site in 10 minutes

This guide shows the minimal steps to embed a live auction listing
on any website using the Property Commerce Platform SDK.

## Prerequisites

- A Property Commerce Platform account (sign up at app.propertycommerce.io)
- Your Tenant ID and API key from the dashboard
- Any website with an HTML page you can edit

## Step 1 — Add the SDK

Paste this before your closing `</body>` tag:

```html
<script>
  window.PCPConfig = {
    apiUrl:   'https://api.propertycommerce.io',   // or your self-hosted URL
    wsUrl:    'wss://api.propertycommerce.io/ws',
    tenantId: 'YOUR_TENANT_ID',
    currency: 'USD',
    theme: {
      primaryColor: 'inherit',   // uses your site's primary colour
      fontFamily:   'inherit',   // uses your site's font
    }
  };
</script>
<script src="https://cdn.propertycommerce.io/sdk/v2/pcp.min.js" defer></script>
```

## Step 2 — Add a container element

Where you want the auction listing to appear, add:

```html
<div id="auction-listings"></div>
```

## Step 3 — Mount the widget

```html
<script>
  document.addEventListener('DOMContentLoaded', () => {
    PCP.mount('auction-listing', '#auction-listings');
  });
</script>
```

That's it. The listing widget fetches live auctions from your account
and renders them inside the div, inheriting your page's styles.

## Mount a live auction room

When a bidder clicks a lot, you can open the live auction room on a
dedicated page:

```html
<!-- auction-room.html?lot=UUID -->
<div id="auction-room"></div>
<script>
  const lotId = new URLSearchParams(location.search).get('lot');
  PCP.mount('auction-room', '#auction-room', { lotId });
</script>
```

## Next steps

- [WordPress plugin](wordpress-plugin.md) — install via the WP plugin directory
- [Authentication](authentication.md) — let users register and log in
- [Webhooks](webhooks.md) — receive real-time events in your backend
- [SDK reference](sdk-reference.md) — all mount options and PCP API
