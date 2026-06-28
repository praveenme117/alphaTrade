package com.trading.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * Redis-backed rate limiter configuration.
 *
 * Strategy: per-authenticated-user (X-User-Id header injected by JwtAuthFilter).
 * Falls back to IP address for unauthenticated requests (login/register).
 *
 * Limits:
 *   replenishRate  = 20 tokens/second (steady-state)
 *   burstCapacity  = 50 tokens (max burst)
 *   requestedTokens = 1 per request
 */
@Configuration
public class RateLimiterConfig {

    /**
     * Default rate limiter: 20 req/sec, burst of 50.
     */
    @Bean
    @Primary
    public RedisRateLimiter defaultRateLimiter() {
        return new RedisRateLimiter(20, 50, 1);
    }

    /**
     * Strict rate limiter for auth endpoints: 5 req/sec (prevent brute force).
     */
    @Bean("authRateLimiter")
    public RedisRateLimiter authRateLimiter() {
        return new RedisRateLimiter(5, 10, 1);
    }

    /**
     * Key resolver: use X-User-Id if authenticated, else fall back to IP.
     */
    @Bean
    @Primary
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null) {
                return Mono.just("user:" + userId);
            }
            // Fallback to IP for unauthenticated requests
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just("ip:" + ip);
        };
    }
}
