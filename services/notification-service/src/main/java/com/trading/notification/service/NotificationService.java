package com.trading.notification.service;

import com.trading.notification.entity.Notification;
import com.trading.notification.repository.NotificationRepository;
import com.trading.shared.events.PaymentCompletedEvent;
import com.trading.shared.events.TradeExecutedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // ─── Kafka consumers ──────────────────────────────────────

    /**
     * Trade executed → "Your order was filled" notification.
     */
    @KafkaListener(topics = "trade.executions", groupId = "notification-service-trades")
    @Transactional
    public void onTradeExecuted(TradeExecutedEvent event) {
        String side = event.getSide().name();
        BigDecimal qty   = event.getQuantity().setScale(4, RoundingMode.HALF_UP);
        BigDecimal price = event.getPrice().setScale(2, RoundingMode.HALF_UP);

        String title = String.format("Order Filled — %s %s", side, event.getSymbol());
        String message = String.format(
                "%s %.4f %s @ ₹%.2f | Total: ₹%.2f | Fee: ₹%.4f",
                side, qty, event.getSymbol(), price,
                event.getTotalValue(), event.getFee());

        String metadata = String.format(
                "{\"orderId\":\"%s\",\"symbol\":\"%s\",\"side\":\"%s\"}",
                event.getOrderId(), event.getSymbol(), side);

        save(event.getUserId(), "TRADE_EXECUTED", title, message, metadata);
        log.info("Notification created: TRADE_EXECUTED for user {}", event.getUserId());
    }

    /**
     * Payment completed → "Funds added / Withdrawal processed" notification.
     */
    @KafkaListener(topics = "payments.completed", groupId = "notification-service-payments")
    @Transactional
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        boolean isDeposit = "DEPOSIT".equals(event.getType());
        String type  = isDeposit ? "DEPOSIT_SUCCESS" : "WITHDRAWAL_SUCCESS";
        String title = isDeposit ? "Funds Added to Wallet" : "Withdrawal Processed";
        String message = String.format(
                "%s of %s %.2f via %s was %s.",
                isDeposit ? "Deposit" : "Withdrawal",
                event.getCurrency(), event.getAmount(),
                event.getGateway(),
                isDeposit ? "credited to your wallet" : "initiated successfully");

        String metadata = String.format(
                "{\"paymentOrderId\":\"%s\",\"amount\":%.2f,\"currency\":\"%s\"}",
                event.getPaymentOrderId(), event.getAmount(), event.getCurrency());

        save(event.getUserId(), type, title, message, metadata);
    }

    // ─── Read operations ──────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<Notification> getNotifications(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public long countUnread(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markRead(UUID userId, UUID notificationId) {
        notificationRepository.findByIdAndUserId(notificationId, userId)
                .ifPresent(n -> {
                    n.setRead(true);
                    notificationRepository.save(n);
                });
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.markAllReadForUser(userId);
    }

    // ─── Helpers ─────────────────────────────────────────────

    private void save(UUID userId, String type, String title, String message, String metadata) {
        notificationRepository.save(Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .metadata(metadata)
                .build());
    }
}
