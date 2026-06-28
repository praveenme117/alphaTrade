package com.trading.payment.service;

import com.trading.payment.dto.DepositRequest;
import com.trading.payment.dto.WithdrawRequest;
import com.trading.payment.entity.PaymentOrder;
import com.trading.payment.gateway.PaymentGatewayPort;
import com.trading.payment.gateway.impl.MockPaymentGateway;
import com.trading.payment.repository.PaymentOrderRepository;
import com.trading.shared.enums.PaymentStatus;
import com.trading.shared.events.PaymentCompletedEvent;
import com.trading.shared.exception.TradingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Unit Tests")
class PaymentServiceTest {

    @Mock private PaymentOrderRepository repo;
    @Mock private KafkaTemplate<String, PaymentCompletedEvent> kafkaTemplate;

    private PaymentService paymentService;
    private MockPaymentGateway mockGateway;

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockGateway = new MockPaymentGateway();
        Map<String, PaymentGatewayPort> gateways = Map.of("MOCK", mockGateway);
        paymentService = new PaymentService(repo, kafkaTemplate, gateways);
        ReflectionTestUtils.setField(paymentService, "activeGateway", "MOCK");
    }

    private PaymentOrder savedOrder(UUID id, PaymentStatus status) {
        return PaymentOrder.builder()
                .id(id).userId(USER_ID)
                .direction("DEPOSIT").amount(BigDecimal.valueOf(5000))
                .currency("INR").status(status)
                .gateway("MOCK")
                .idempotencyKey("idem-" + id)
                .build();
    }

    // ─── Deposit ──────────────────────────────────────────────

    @Test
    @DisplayName("deposit: processes new deposit and returns COMPLETED order")
    void deposit_ProcessesSuccessfully() {
        DepositRequest req = new DepositRequest();
        req.setAmount(BigDecimal.valueOf(5000));
        req.setCurrency("INR");
        req.setIdempotencyKey(UUID.randomUUID().toString());

        UUID orderId = UUID.randomUUID();
        when(repo.findByIdempotencyKey(req.getIdempotencyKey())).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> {
            PaymentOrder o = inv.getArgument(0);
            if (o.getId() == null) ReflectionTestUtils.setField(o, "id", orderId);
            return o;
        });

        PaymentOrder result = paymentService.deposit(USER_ID, req);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(result.getGateway()).isEqualTo("MOCK");
        assertThat(result.getGatewayReference()).startsWith("MOCK-DEP-");
        verify(kafkaTemplate).send(eq("payments.completed"), eq(USER_ID.toString()), any());
    }

    @Test
    @DisplayName("deposit: returns existing order on idempotency key hit (no duplicate processing)")
    void deposit_IdempotencyKeyHit_ReturnsCachedOrder() {
        String idempotencyKey = "same-key-123";
        DepositRequest req = new DepositRequest();
        req.setAmount(BigDecimal.valueOf(5000));
        req.setCurrency("INR");
        req.setIdempotencyKey(idempotencyKey);

        UUID existingId = UUID.randomUUID();
        PaymentOrder existing = savedOrder(existingId, PaymentStatus.COMPLETED);
        when(repo.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existing));

        PaymentOrder result = paymentService.deposit(USER_ID, req);

        assertThat(result.getId()).isEqualTo(existingId);
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        verify(repo, never()).save(any());     // No duplicate DB write
        verify(kafkaTemplate, never()).send(any(), any(), any()); // No duplicate event
    }

    @Test
    @DisplayName("deposit: throws TradingException for amount below minimum")
    void deposit_ThrowsForInvalidAmount() {
        DepositRequest req = new DepositRequest();
        req.setAmount(BigDecimal.valueOf(0.5));  // below ₹1 minimum
        req.setCurrency("INR");
        req.setIdempotencyKey(UUID.randomUUID().toString());

        when(repo.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> {
            PaymentOrder o = inv.getArgument(0);
            ReflectionTestUtils.setField(o, "id", UUID.randomUUID());
            return o;
        });

        assertThatThrownBy(() -> paymentService.deposit(USER_ID, req))
                .isInstanceOf(TradingException.class)
                .hasMessageContaining("Payment failed");
    }

    // ─── Withdrawal ───────────────────────────────────────────

    @Test
    @DisplayName("withdraw: processes new withdrawal and returns COMPLETED order")
    void withdraw_ProcessesSuccessfully() {
        WithdrawRequest req = new WithdrawRequest();
        req.setAmount(BigDecimal.valueOf(2000));
        req.setCurrency("INR");
        req.setBankReference("UPI-TEST-1234");
        req.setIdempotencyKey(UUID.randomUUID().toString());

        when(repo.findByIdempotencyKey(req.getIdempotencyKey())).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> {
            PaymentOrder o = inv.getArgument(0);
            if (o.getId() == null) ReflectionTestUtils.setField(o, "id", UUID.randomUUID());
            return o;
        });

        PaymentOrder result = paymentService.withdraw(USER_ID, req);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(result.getGatewayReference()).startsWith("MOCK-WDR-");
        // Withdrawal does NOT publish payment event (no wallet credit)
    }

    @Test
    @DisplayName("withdraw: idempotent — returns same result on duplicate key")
    void withdraw_IdempotencyKeyHit() {
        String key = "wdr-idem-999";
        WithdrawRequest req = new WithdrawRequest();
        req.setAmount(BigDecimal.valueOf(1000));
        req.setCurrency("INR");
        req.setBankReference("UPI-123");
        req.setIdempotencyKey(key);

        PaymentOrder existing = savedOrder(UUID.randomUUID(), PaymentStatus.COMPLETED);
        existing.setDirection("WITHDRAWAL");
        when(repo.findByIdempotencyKey(key)).thenReturn(Optional.of(existing));

        PaymentOrder result = paymentService.withdraw(USER_ID, req);
        assertThat(result.getDirection()).isEqualTo("WITHDRAWAL");
        verify(repo, never()).save(any());
    }

    // ─── History ──────────────────────────────────────────────

    @Test
    @DisplayName("getById: throws ResourceNotFoundException for another user's payment")
    void getById_ThrowsForWrongUser() {
        UUID paymentId = UUID.randomUUID();
        PaymentOrder order = savedOrder(paymentId, PaymentStatus.COMPLETED);
        order.setUserId(UUID.randomUUID()); // different user!
        when(repo.findById(paymentId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.getById(USER_ID, paymentId))
                .hasMessageContaining("PaymentOrder");
    }
}
