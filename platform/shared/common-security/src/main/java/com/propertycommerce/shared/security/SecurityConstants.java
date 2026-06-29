package com.propertycommerce.shared.security;

public final class SecurityConstants {
    private SecurityConstants() {}

    public static final String ROLE_GUEST = "GUEST";
    public static final String ROLE_HOST = "HOST";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_SUPERADMIN = "SUPERADMIN";

    public static final String[] PUBLIC_ENDPOINTS = {
        "/api/v1/auth/**",
        "/api/v1/properties/search",
        "/api/v1/properties/{id}",
        "/api/v1/properties/{id}/availability",
        "/api/v1/properties/{id}/reviews",
        "/api/v1/ai/concierge/public",
        "/actuator/health",
        "/actuator/info",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/webhook/**"
    };
}
