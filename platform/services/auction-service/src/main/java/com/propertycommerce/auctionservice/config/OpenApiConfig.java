package com.propertycommerce.auctionservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Phase C — Open API layer.
 *
 * Exposes:
 *   GET /v3/api-docs           machine-readable OpenAPI 3.1 spec
 *   GET /swagger-ui.html       interactive Swagger UI
 *
 * Both paths are listed in JwtAuthGatewayFilter.PUBLIC_PATHS so they are
 * reachable without a token — anonymous developers exploring the API
 * should not need credentials to read the documentation.
 *
 * Two security schemes are documented here because the gateway accepts
 * either: a tenant-scoped JWT (end-user login) or a long-lived API key
 * (server-to-server / WordPress plugin / VS Code extension usage).
 * See platform/infrastructure/api-gateway ApiKeyGatewayFilter.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Property Commerce Platform — Auction Service",
                version = "2.0.0",
                description = """
                        Live bidding, proxy/anti-snipe logic, bidding credentials,
                        bidder Q&A, KYC, and AI fraud assessment.

                        This service is one of twelve independently deployable
                        microservices that make up the Property Commerce Platform.
                        All requests are routed through the API gateway at
                        /api/v1/auctions/** and require either a Bearer JWT
                        (end-user session) or an X-Api-Key header (tenant
                        server-to-server integration).
                        """,
                contact = @Contact(name = "Property Commerce Platform", url = "https://propertycommerce.io")
        ),
        servers = {
                @Server(url = "https://api.propertycommerce.io", description = "Production"),
                @Server(url = "http://localhost:8080", description = "Local (via API gateway)")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "End-user JWT obtained from POST /api/v1/auth/login"
)
@SecurityScheme(
        name = "apiKeyAuth",
        type = SecuritySchemeType.APIKEY,
        paramName = "X-Api-Key",
        in = io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.HEADER,
        description = "Tenant API key for server-to-server integrations. Issued via POST /api/v1/tenants/{id}/api-keys"
)
public class OpenApiConfig {
}
