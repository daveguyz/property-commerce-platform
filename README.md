# Property Commerce Platform

An open, embeddable property commerce engine — auction, booking, listing, and
purchase agreement infrastructure that installs on any existing website.

## What it is

Property Commerce Platform (PCP) provides the full real estate transaction
stack as a set of independently deployable microservices and a lightweight
JavaScript SDK. Host sites embed a single `<script>` tag; the SDK mounts
interactive auction rooms, listing grids, booking forms, and account dashboards
into whatever HTML elements the host site provides — inheriting the host's
fonts, colours, and layout.

## Architecture

```
property-commerce-platform/
├── platform/               Spring Boot microservices (Java 21)
│   ├── shared/             common-dto, common-events, common-security
│   ├── infrastructure/     api-gateway, service-discovery, config-server
│   └── services/           12 independently deployable services
├── sdk/                    Embeddable JavaScript SDK (Phase B)
│   ├── packages/           core, auction-widget, listing-widget, ...
│   ├── adapters/           wordpress, webflow, vanilla
│   └── demo/               Reference integration (plain HTML)
├── integrations/           Optional platform adapters
│   └── shopify/            Shopify App (legacy — kept for migration)
├── vscode-extension/       VS Code Marketplace extension (Phase J)
├── docs/                   OpenAPI specs, integration guides
└── docker/                 Docker Compose + Kubernetes manifests
```

## Microservices

| Service | Port | Responsibility |
|---|---|---|
| api-gateway | 8080 | JWT auth, rate limiting, routing |
| auth-service | 8091 | Registration, login, roles, JWT |
| auction-service | 8094 | Lots, bids, credentials, Q&A |
| booking-engine | 8082 | Bookings, purchase agreements |
| property-service | 8081 | Property listings, availability |
| payment-service | 8083 | Stripe integration, deposits |
| notification-service | 8087 | Email, SMS, push |
| messaging-service | 8093 | Conversations, support tickets |
| search-service | 8088 | Elasticsearch listing search |
| pricing-engine | 8085 | Dynamic pricing rules |
| ai-service | 8084 | Fraud detection, AI search |
| analytics-service | 8092 | Events, dashboards |
| trust-service | 8086 | Reputation scores, KYC |

## Quick start (local dev)

```bash
# 1. Clone
git clone https://github.com/daveguyz/property-commerce-platform.git
cd property-commerce-platform

# 2. Start infrastructure (Postgres, Redis, Kafka, Elasticsearch)
docker compose -f docker/docker-compose.yml up -d

# 3. Build shared modules
mvn install -pl platform/shared/common-dto,platform/shared/common-events,platform/shared/common-security -am -DskipTests

# 4. Run a service
cd platform/services/auction-service
mvn spring-boot:run
```

## Embed on any website

```html
<script>
  window.PCPConfig = {
    apiUrl:   'https://api.yourplatform.com',
    wsUrl:    'wss://api.yourplatform.com/ws',
    tenantId: 'your-tenant-uuid',
    currency: 'USD',
  };
</script>
<script src="https://cdn.propertycommerce.io/sdk/v2/pcp.min.js"></script>

<!-- Mount the auction listing into any div -->
<div id="auction-listings"></div>
<script>
  PCP.mount('auction-listing', '#auction-listings');
</script>
```

## Documentation

- [Quickstart guide](docs/guides/quickstart.md)
- [API reference](docs/openapi/)
- [WordPress plugin](docs/guides/wordpress-plugin.md)
- [Authentication](docs/guides/authentication.md)
- [Webhooks](docs/guides/webhooks.md)

## Migrated from

This project was migrated from `staysphere-aos` (a Shopify-specific property
auction operating system). The Spring Boot backend is fully preserved; the
Shopify Liquid theme has been replaced by the platform-agnostic JavaScript SDK.

## Licence

Apache 2.0 — see [LICENSE](LICENSE)
