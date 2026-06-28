package com.trading.order.entity;

import com.trading.shared.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders",
       indexes = {
           @Index(name = "idx_orders_user_id", columnList = "user_id"),
           @Index(name = "idx_orders_symbol",  columnList = "symbol"),
           @Index(name = "idx_orders_status",  columnList = "status")
       })
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "instrument_id", nullable = false)
    private UUID instrumentId;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 20)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrderSide side;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 20)
    private ProductType productType;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal quantity;

    /** Requested price (null for MARKET orders) */
    @Column(precision = 20, scale = 8)
    private BigDecimal price;

    /** Stop trigger price for STOP_LOSS / STOP_LIMIT */
    @Column(name = "stop_price", precision = 20, scale = 8)
    private BigDecimal stopPrice;

    /** Quantity filled so far */
    @Column(name = "filled_quantity", precision = 20, scale = 8)
    @Builder.Default
    private BigDecimal filledQuantity = BigDecimal.ZERO;

    /** Average fill price */
    @Column(name = "average_price", precision = 20, scale = 8)
    private BigDecimal averagePrice;

    /** Total fees charged */
    @Column(precision = 20, scale = 8)
    @Builder.Default
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
