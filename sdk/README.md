# @property-commerce/sdk

Embeddable property commerce widgets — auctions, listings, bookings, accounts.

## Script tag (no build step)

```html
<script>
  window.PCPConfig = { apiUrl: 'https://api.propertycommerce.io', tenantId: 'YOUR_TENANT_ID' };
</script>
<script src="https://cdn.propertycommerce.io/sdk/v2/pcp.min.js"></script>
<div id="listings"></div>
<script>PCP.mount('auction-listing', '#listings');</script>
```

## npm

```bash
npm install @property-commerce/sdk
```

```ts
import PCP from '@property-commerce/sdk';
await PCP.init({ apiUrl: '...', tenantId: '...' });
PCP.mount('auction-listing', '#listings', { roomUrl: '/room?lot={lotId}' });
```

Full docs: https://github.com/daveguyz/property-commerce-platform/tree/main/docs
