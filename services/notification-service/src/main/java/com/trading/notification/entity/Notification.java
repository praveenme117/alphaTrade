package com.trading.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Persisted in-app notification.
 * Created by Kafka consumers — one per significant trading event.
 */
@Entity
@Table(name = "notifications",
    indexes = {
        @Index(name = "idx_notifications_user_id", columnList = "user_id"),
        @Index(name = "idx_notifications_read",    columnList = "user_id, is_read")
    })
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * Notification type — drives the icon shown in the UI.
     * TRADE_EXECUTED, ORDER_PLACED, ORDER_CANCELLED, DEPOSIT_SUCCESS,
     * WITHDRAWAL_SUCCESS, PRICE_ALERT, SYSTEM
     */
    @Column(nullable = false, length = 40)
    private String type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    /** Deep-link data for the frontend (e.g., orderId, symbol) */
    @Column(length = 200)
    private String metadata;

    @Column(name = "is_read")
    @Builder.Default
    private boolean read = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
