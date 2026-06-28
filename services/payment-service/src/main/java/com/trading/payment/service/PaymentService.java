package com.trading.payment.service;

import com.trading.payment.dto.DepositRequest;
import com.trading.payment.dto.WithdrawRequest;
import com.trading.payment.entity.PaymentOrder;
import com.trading.payment.gateway.PaymentGatewayPort;
import com.trading.payment.repository.PaymentOrderRepository;
import com.trading.shared.enums.PaymentStatus;
import com.trading.shared.events.PaymentCompletedEvent;
import com.trading.shared.exception.ResourceNotFoundException;
import com.trading.shared.exception.TradingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Payment service — orchestrates deposit/withdrawal via the active gateway.
 *
 * Flow:
 *   1. Idempotency check (return existing if key already used)
 *   2. Create PaymentOrder (PENDING)
 *   3. Call gateway (blocking — mock is fast, real gateways would be async)
 *   4. Update status → COMPLETED | FAILED
 *   5. On success → publish PaymentCompletedEvent → Kafka → wallet-service credits
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String TOPIC_PAYMENTS = "payments.completed";

    private final PaymentOrderRepository paymentOrderRepository;
    private final KafkaTemplate<String, PaymentCompletedEvent> kafkaTemplate;
    private final Map<String, PaymentGatewayPort> gateways;   // Spring injects by bean name

    @Value("${payment.gateway:MOCK}")
    private String activeGateway;

    // ─── Deposit ──────────────────────────────────────────────

    @Transactional
    public PaymentOrder deposit(UUID userId, DepositRequest req) {
        // 1. Idempotency check
        return paymentOrderRepository.findByIdempotencyKey(req.getIdempotencyKey())
                .map(existing -> {
                    log.info("Idempotent deposit hit: key={} id={}", req.getIdempotencyKey(), existing.getId());
                    return existing;
                })
                .orElseGet(() -> processDeposit(userId, req));
    }

    private PaymentOrder processDeposit(UUID userId, DepositRequest req) {
        PaymentGatewayPort gateway = resolveGateway();

        // 2. Create PENDING order
        PaymentOrder order = paymentOrderRepository.save(PaymentOrder.builder()
                .userId(userId)
                .direction("DEPOSIT")
                .amount(req.getAmount())
                .currency(req.getCurrency())
                .status(PaymentStatus.CREATED)
                .gateway(gateway.gatewayName())
                .idempotencyKey(req.getIdempotencyKey())
                .build());

        // 3. Call gateway
        PaymentGatewayPort.GatewayResult result = gateway.deposit(
                userId, req.getAmount(), req.getCurrency(),
                req.getIdempotencyKey(), "Deposit by user " + userId);

        // 4. Update status
        if (result.success()) {
            order.setStatus(PaymentStatus.COMPLETED);
            order.setGatewayReference(result.gatewayReference());
            order.setGatewayResponse(result.response());
            order.setProcessedAt(Instant.now());
            paymentOrderRepository.save(order);

            // 5. Notify wallet-service
            publishPaymentCompleted(order, userId);
            log.info("Deposit COMPLETED: user={} amount={} ref={}", userId, req.getAmount(), result.gatewayReference());
        } else {
            order.setStatus(PaymentStatus.FAILED);
            order.setGatewayResponse(result.response());
            paymentOrderRepository.save(order);
            log.warn("Deposit FAILED: user={} reason={}", userId, result.response());
            throw new TradingException("Payment failed: " + result.response(),
                    result.errorCode(), 402);
        }

        return order;
    }

    // ─── Withdrawal ───────────────────────────────────────────

    @Transactional
    public PaymentOrder withdraw(UUID userId, WithdrawRequest req) {
        return paymentOrderRepository.findByIdempotencyKey(req.getIdempotencyKey())
                .map(existing -> {
                    log.info("Idempotent withdrawal hit: key={}", req.getIdempotencyKey());
                    return existing;
                })
                .orElseGet(() -> processWithdrawal(userId, req));
    }

    private PaymentOrder processWithdrawal(UUID userId, WithdrawRequest req) {
        PaymentGatewayPort gateway = resolveGateway();

        PaymentOrder order = paymentOrderRepository.save(PaymentOrder.builder()
                .userId(userId)
                .direction("WITHDRAWAL")
                .amount(req.getAmount())
                .currency(req.getCurrency())
                .status(PaymentStatus.CREATED)
                .gateway(gateway.gatewayName())
                .idempotencyKey(req.getIdempotencyKey())
                .bankReference(req.getBankReference())
                .build());

        PaymentGatewayPort.GatewayResult result = gateway.withdraw(
                userId, req.getAmount(), req.getCurrency(),
                req.getBankReference(), req.getIdempotencyKey());

        if (result.success()) {
            order.setStatus(PaymentStatus.COMPLETED);
            order.setGatewayReference(result.gatewayReference());
            order.setGatewayResponse(result.response());
            order.setProcessedAt(Instant.now());
            paymentOrderRepository.save(order);
            log.info("Withdrawal COMPLETED: user={} amount={} ref={}",
                    userId, req.getAmount(), result.gatewayReference());
        } else {
            order.setStatus(PaymentStatus.FAILED);
            order.setGatewayResponse(result.response());
            paymentOrderRepository.save(order);
            throw new TradingException("Withdrawal failed: " + result.response(),
                    result.errorCode(), 402);
        }

        return order;
    }

    // ─── History ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<PaymentOrder> getHistory(UUID userId, Pageable pageable) {
        return paymentOrderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public PaymentOrder getById(UUID userId, UUID paymentId) {
        return paymentOrderRepository.findById(paymentId)
                .filter(p -> p.getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("PaymentOrder", paymentId.toString()));
    }

    // ─── Helpers ─────────────────────────────────────────────

    private PaymentGatewayPort resolveGateway() {
        PaymentGatewayPort gateway = gateways.get(activeGateway);
        if (gateway == null) {
            log.warn("Gateway '{}' not found, falling back to MOCK", activeGateway);
            gateway = gateways.get("MOCK");
        }
        return gateway;
    }

    private void publishPaymentCompleted(PaymentOrder order, UUID userId) {
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .paymentOrderId(order.getId())
                .userId(userId)
                .amount(order.getAmount())
                .currency(order.getCurrency())
                .gateway(order.getGateway())
                .type(order.getDirection())           // shared event uses 'type' field
                .completedAt(order.getProcessedAt())
                .build();
        kafkaTemplate.send(TOPIC_PAYMENTS, userId.toString(), event);
    }
}
