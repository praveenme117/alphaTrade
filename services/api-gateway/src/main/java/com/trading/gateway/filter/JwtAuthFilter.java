package com.trading.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.gateway.security.JwtValidator;
import com.trading.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * JWT Authentication Gateway Filter.
 *
 * Applied per-route to protected endpoints.
 * - Extracts Bearer token from Authorization header
 * - Validates JWT signature + expiry
 * - Injects X-User-Id, X-User-Role, X-User-Email as trusted headers downstream
 * - Returns 401 on missing / invalid / expired token
 *
 * Public routes (auth endpoints) do NOT use this filter.
 */
@Slf4j
@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    private final JwtValidator jwtValidator;
    private final ObjectMapper objectMapper;

    public JwtAuthFilter(JwtValidator jwtValidator, ObjectMapper objectMapper) {
        super(Config.class);
        this.jwtValidator = jwtValidator;
        this.objectMapper = objectMapper;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String token = extractToken(exchange.getRequest());

            if (token == null) {
                return unauthorizedResponse(exchange, "Missing Authorization header");
            }

            if (!jwtValidator.isValid(token)) {
                return unauthorizedResponse(exchange, "Invalid or expired token");
            }

            // Token is valid — inject user context headers for downstream services
            String userId = jwtValidator.extractUserId(token);
            String role   = jwtValidator.extractRole(token);
            String email  = jwtValidator.extractEmail(token);

            ServerHttpRequest mutatedRequest = exchange.getRequest()
                    .mutate()
                    .header("X-User-Id",    userId)
                    .header("X-User-Role",  role)
                    .header("X-User-Email", email)
                    // Remove raw Authorization header to prevent downstream re-validation
                    // (downstream services trust X-User-* headers)
                    .build();

            log.debug("Gateway: authenticated user={} role={}", userId, role);
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    private String extractToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ApiResponse<Void> body = ApiResponse.error(message, "UNAUTHORIZED");
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            bytes = "{\"success\":false,\"message\":\"Unauthorized\"}".getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    public static class Config {
        // Reserved for future per-route configuration (e.g. required roles)
    }
}
