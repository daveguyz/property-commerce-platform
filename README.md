# 🏠 StaySphere AOS — Accommodation Operating System

**The premier accommodation booking platform for Namibia and Southern Africa, built on Spring Boot microservices + Shopify.**

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│                   Shopify Theme (Liquid)                  │
│         + StaySphere JS/CSS (AI Concierge, Search)       │
└────────────────────────┬────────────────────────────────┘
                         │ HTTPS
┌────────────────────────▼────────────────────────────────┐
│              API Gateway (Port 8080)                     │
│    JWT Auth • Rate Limiting • CORS • Load Balancing      │
└────┬──────┬──────┬──────┬──────┬──────┬──────┬─────────┘
     │      │      │      │      │      │      │
  8081   8082   8083   8084   8085   8086   8087   8088
Property Booking  Pay   AI   Pricing Trust  Notif  Search
Service  Engine  Svc   Svc   Engine  Svc    Svc   Service
   │       │      │      │      │      │      │      │
  PG     PG    PG    PG    PG    PG    PG    ES
  Redis       Stripe Anthropic          Twilio
              ←────────── Kafka ──────────────→
              ←──────── Eureka (8761) ─────────→
              ←────── Config Server (8888) ──────→
```

## 🚀 Quick Start

```bash
# 1. Clone and configure
cp .env.example .env
# Edit .env with your Stripe, Anthropic, Shopify, Twilio keys

# 2. Start everything
docker compose up -d

# 3. Wait for services to start (~3-5 minutes)
docker compose ps

# 4. Access points:
# - API Gateway:      http://localhost:8080
# - Eureka Dashboard: http://localhost:8761  (eureka/eureka_secret)
# - Kafka UI:         http://localhost:8989
# - Grafana:          http://localhost:3000  (admin/grafana_secret)
# - Prometheus:       http://localhost:9090
```

## 📦 Microservices

| Service | Port | Description |
|---------|------|-------------|
| api-gateway | 8080 | Spring Cloud Gateway, JWT auth, rate limiting |
| service-discovery | 8761 | Eureka Server |
| config-server | 8888 | Spring Cloud Config |
| property-service | 8081 | Listings, availability, Shopify product sync |
| booking-engine | 8082 | Reservations, SERIALIZABLE transactions, AI negotiation |
| payment-service | 8083 | Stripe Connect, webhooks, host payouts |
| ai-service | 8084 | Claude-powered concierge, search intent, area intelligence |
| pricing-engine | 8085 | Namibia-aware dynamic pricing, hourly recalculation |
| trust-service | 8086 | 100-pt trust scores, review management |
| notification-service | 8087 | Email (Thymeleaf), SMS + WhatsApp (Twilio) |
| search-service | 8088 | Elasticsearch full-text + geo search |
| shopify-integration | 8090 | Webhook handlers, storefront sync |

## 🔐 Security Features

- **JWT authentication** across all services (RS256-compatible)
- **API Gateway** validates tokens before forwarding requests
- **Rate limiting** via Redis: 120 req/min general, 20 req/min AI endpoints
- **Stripe webhook HMAC** verification
- **Shopify webhook HMAC-SHA256** verification
- **`Isolation.SERIALIZABLE`** on booking transactions (prevents double bookings)
- **Redis locks** (15-min TTL) as secondary protection layer
- **CORS whitelist** for Shopify domains only
- **Flyway migrations** for schema integrity

## 💰 Pricing Logic

Namibia-specific dynamic pricing factors:
- **Seasonal**: Jun–Oct (dry/wildlife season) +25%, Feb–Apr (rainy) -10%
- **Day-of-week**: Friday/Saturday +15%
- **Custom rules**: per-property date range and event rules
- **Floor price**: never drops below host-set minimum
- **Multiplier cap**: 0.5x–1.5x base rate
- **Hourly recalculation**: `@Scheduled(fixedRate = 3_600_000)`

## 🤖 AI Features (Claude)

- **Travel Concierge**: full multi-turn conversation with property recommendations
- **Search Intent Extraction**: natural language → structured `SearchRequestDTO`
- **AI Negotiation Advisor**: Claude advises hosts on guest price offers
- **Smart Calendar Insights**: booking timing recommendations
- **Area Intelligence**: local knowledge about Namibian destinations
- **Property Comparison**: structured comparison with recommendation

## 📊 Monitoring

- **Prometheus** scrapes all `/actuator/prometheus` endpoints
- **Grafana** for dashboards (pre-configured)
- **Kafka UI** for message inspection
- **Spring Actuator** health, info, metrics on all services

## 🛒 Shopify Integration

- Products synced from property-service via Admin API
- Metafields: property_id, bedrooms, max_guests, lat/lon, city
- Order webhooks → booking confirmation
- Customer webhooks → user profile sync
- Draft orders for payment processing via Stripe Connect

## 📁 Project Structure

```
staysphere/
├── shared/
│   ├── common-dto/          # Shared DTOs (PropertyDTO, BookingDTO, etc.)
│   ├── common-events/       # Kafka event classes (TOPIC constants)
│   └── common-security/     # JWT filter, token provider
├── infrastructure/
│   ├── api-gateway/         # Spring Cloud Gateway
│   ├── service-discovery/   # Eureka Server
│   └── config-server/       # Spring Cloud Config
├── services/
│   ├── property-service/    # Port 8081
│   ├── booking-engine/      # Port 8082
│   ├── payment-service/     # Port 8083
│   ├── ai-service/          # Port 8084
│   ├── pricing-engine/      # Port 8085
│   ├── trust-service/       # Port 8086
│   ├── notification-service/# Port 8087
│   └── search-service/      # Port 8088
├── shopify-integration/     # Webhook handlers + Storefront client
├── shopify-theme/           # Liquid templates + JS/CSS
├── k8s/                     # Kubernetes manifests + monitoring config
├── docker-compose.yml       # Full local stack
└── .env.example             # Environment variable template
```

## 🏃 Running Tests

```bash
# Run all tests (requires Docker for Testcontainers)
mvn test

# Run specific service tests
mvn test -pl services/booking-engine
```

## 🌍 Target Market

- **Primary**: Namibia (NAD currency, local knowledge)
- **Secondary**: Southern Africa (Botswana, Zambia, South Africa)
- **Key destinations**: Windhoek, Swakopmund, Etosha National Park, Sossusvlei, Fish River Canyon

---

Built with ❤️ for Africa's most spectacular destinations.
