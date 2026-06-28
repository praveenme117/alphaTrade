package com.trading.market.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket STOMP configuration.
 *
 * Connection: ws://localhost:8082/ws   (or wss:// in production)
 *
 * Subscriptions:
 *   /topic/prices/{symbol}   — live price tick for a symbol
 *   /topic/prices/all        — all symbol price ticks (for dashboard watchlist)
 *
 * Frontend usage (SockJS fallback):
 *   const client = new Client({ brokerURL: 'ws://localhost:8080/ws' });
 *   client.subscribe('/topic/prices/BTC', msg => { ... });
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();                  // SockJS fallback for browsers without native WS
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Client subscribes to: /topic/prices/BTC
        registry.enableSimpleBroker("/topic", "/queue");
        // Client sends messages to: /app/...
        registry.setApplicationDestinationPrefixes("/app");
    }
}
