package com.trading.gateway.config;

import com.trading.gateway.filter.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties;

import java.util.HashSet;
import java.util.Set;

/**
 * Defines all gateway routes and wires the JWT filter on protected routes.
 *
 * Route design:
 *  - Public:    /api/v1/auth/register|login|refresh → auth-service (no JWT)
 *  - Protected: all other /api/v1/** → respective service (JWT filter applied)
 *  - Swagger:   /docs/{service}/v3/api-docs → per-service swagger
 *  - WebSocket: /ws/** → market-service
 */
@Configuration
public class GatewayRoutesConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Value("${services.auth-service.url}")
    private String authServiceUrl;

    @Value("${services.market-service.url}")
    private String marketServiceUrl;

    @Value("${services.order-service.url}")
    private String orderServiceUrl;

    @Value("${services.portfolio-service.url}")
    private String portfolioServiceUrl;

    @Value("${services.wallet-service.url}")
    private String walletServiceUrl;

    @Value("${services.payment-service.url}")
    private String paymentServiceUrl;

    @Value("${services.notification-service.url}")
    private String notificationServiceUrl;

    @Value("${services.ai-assistant-service.url}")
    private String aiAssistantServiceUrl;

    public GatewayRoutesConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        var jwtConfig = new JwtAuthFilter.Config();

        return builder.routes()

            // ─── AUTH SERVICE — public (no JWT) ────────────────────────
            .route("auth-register", r -> r
                .path("/api/v1/auth/register")
                .uri(authServiceUrl))
            .route("auth-login", r -> r
                .path("/api/v1/auth/login")
                .uri(authServiceUrl))
            .route("auth-refresh", r -> r
                .path("/api/v1/auth/refresh")
                .uri(authServiceUrl))

            // ─── AUTH SERVICE — protected ────────────────────────────────
            .route("auth-protected", r -> r
                .path("/api/v1/auth/**")
                .filters(f -> f.filter(jwtAuthFilter.apply(jwtConfig)))
                .uri(authServiceUrl))

            // ─── MARKET SERVICE ──────────────────────────────────────────
            .route("market-service", r -> r
                .path("/api/v1/market/**")
                .filters(f -> f.filter(jwtAuthFilter.apply(jwtConfig)))
                .uri(marketServiceUrl))

            // ─── ORDER SERVICE ───────────────────────────────────────────
            .route("order-service", r -> r
                .path("/api/v1/orders/**")
                .filters(f -> f.filter(jwtAuthFilter.apply(jwtConfig)))
                .uri(orderServiceUrl))

            // ─── PORTFOLIO SERVICE ───────────────────────────────────────
            .route("portfolio-service", r -> r
                .path("/api/v1/portfolio/**")
                .filters(f -> f.filter(jwtAuthFilter.apply(jwtConfig)))
                .uri(portfolioServiceUrl))

            // ─── WALLET SERVICE ──────────────────────────────────────────
            .route("wallet-service", r -> r
                .path("/api/v1/wallet/**")
                .filters(f -> f.filter(jwtAuthFilter.apply(jwtConfig)))
                .uri(walletServiceUrl))

            // ─── PAYMENT SERVICE — protected ─────────────────────────────
            .route("payment-protected", r -> r
                .path("/api/v1/payments/deposit/**",
                      "/api/v1/payments/withdraw/**",
                      "/api/v1/payments/history",
                      "/api/v1/payments/{id}")
                .filters(f -> f.filter(jwtAuthFilter.apply(jwtConfig)))
                .uri(paymentServiceUrl))

            // ─── PAYMENT SERVICE — webhooks (no JWT) ─────────────────────
            .route("payment-webhooks", r -> r
                .path("/api/v1/payments/webhook/**")
                .uri(paymentServiceUrl))

            // ─── AI ASSISTANT SERVICE ────────────────────────────────────
            .route("ai-assistant-service", r -> r
                .path("/api/v1/assistant/**")
                .filters(f -> f.filter(jwtAuthFilter.apply(jwtConfig)))
                .uri(aiAssistantServiceUrl))

            // ─── WEBSOCKET — market live prices ─────────────────────────
            .route("websocket", r -> r
                .path("/ws/**")
                .uri(marketServiceUrl.replace("http://", "ws://")))

            // ─── SWAGGER DOC PROXIES ─────────────────────────────────────
            .route("docs-auth", r -> r
                .path("/docs/auth/v3/api-docs")
                .filters(f -> f.rewritePath("/docs/auth/v3/api-docs", "/v3/api-docs"))
                .uri(authServiceUrl))
            .route("docs-market", r -> r
                .path("/docs/market/v3/api-docs")
                .filters(f -> f.rewritePath("/docs/market/v3/api-docs", "/v3/api-docs"))
                .uri(marketServiceUrl))
            .route("docs-order", r -> r
                .path("/docs/order/v3/api-docs")
                .filters(f -> f.rewritePath("/docs/order/v3/api-docs", "/v3/api-docs"))
                .uri(orderServiceUrl))
            .route("docs-portfolio", r -> r
                .path("/docs/portfolio/v3/api-docs")
                .filters(f -> f.rewritePath("/docs/portfolio/v3/api-docs", "/v3/api-docs"))
                .uri(portfolioServiceUrl))
            .route("docs-wallet", r -> r
                .path("/docs/wallet/v3/api-docs")
                .filters(f -> f.rewritePath("/docs/wallet/v3/api-docs", "/v3/api-docs"))
                .uri(walletServiceUrl))
            .route("docs-payment", r -> r
                .path("/docs/payment/v3/api-docs")
                .filters(f -> f.rewritePath("/docs/payment/v3/api-docs", "/v3/api-docs"))
                .uri(paymentServiceUrl))
            .route("docs-assistant", r -> r
                .path("/docs/assistant/v3/api-docs")
                .filters(f -> f.rewritePath("/docs/assistant/v3/api-docs", "/v3/api-docs"))
                .uri(aiAssistantServiceUrl))

            .build();
    }

    /**
     * Register downstream service API docs with SpringDoc Swagger UI aggregator.
     * Populates the service-switcher dropdown in the Swagger UI.
     */
    @Bean
    @Lazy(false)
    public Set<AbstractSwaggerUiConfigProperties.SwaggerUrl> swaggerUrls() {
        Set<AbstractSwaggerUiConfigProperties.SwaggerUrl> urls = new HashSet<>();
        urls.add(swaggerUrl("auth-service",      "/docs/auth/v3/api-docs"));
        urls.add(swaggerUrl("market-service",    "/docs/market/v3/api-docs"));
        urls.add(swaggerUrl("order-service",     "/docs/order/v3/api-docs"));
        urls.add(swaggerUrl("portfolio-service", "/docs/portfolio/v3/api-docs"));
        urls.add(swaggerUrl("wallet-service",    "/docs/wallet/v3/api-docs"));
        urls.add(swaggerUrl("payment-service",   "/docs/payment/v3/api-docs"));
        urls.add(swaggerUrl("ai-assistant-service", "/docs/assistant/v3/api-docs"));
        return urls;
    }

    private AbstractSwaggerUiConfigProperties.SwaggerUrl swaggerUrl(String name, String url) {
        var swaggerUrl = new AbstractSwaggerUiConfigProperties.SwaggerUrl();
        swaggerUrl.setName(name);
        swaggerUrl.setUrl(url);
        return swaggerUrl;
    }
}
