package com.trading.payment.controller;

import com.trading.payment.dto.DepositRequest;
import com.trading.payment.dto.WithdrawRequest;
import com.trading.payment.entity.PaymentOrder;
import com.trading.payment.service.PaymentService;
import com.trading.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Deposits, withdrawals, transaction history")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Deposit funds (mock gateway — always succeeds)")
    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<PaymentOrder>> deposit(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody DepositRequest request) {
        PaymentOrder order = paymentService.deposit(UUID.fromString(userId), request);
        return ResponseEntity.ok(ApiResponse.ok(order, "Deposit processed successfully"));
    }

    @Operation(summary = "Withdraw funds (mock gateway)")
    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<PaymentOrder>> withdraw(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody WithdrawRequest request) {
        PaymentOrder order = paymentService.withdraw(UUID.fromString(userId), request);
        return ResponseEntity.ok(ApiResponse.ok(order, "Withdrawal processed successfully"));
    }

    @Operation(summary = "Get paginated payment history")
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<PaymentOrder>>> history(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<PaymentOrder> orders = paymentService.getHistory(
                UUID.fromString(userId), PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(orders));
    }

    @Operation(summary = "Get payment by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentOrder>> getById(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getById(UUID.fromString(userId), id)));
    }

    /**
     * Webhook endpoint — no JWT (gateway calls this directly).
     * In production: verify signature before processing.
     */
    @Operation(summary = "Payment gateway webhook (signature-verified in production)")
    @PostMapping("/webhook/{gateway}")
    public ResponseEntity<ApiResponse<?>> webhook(
            @PathVariable String gateway,
            @RequestBody String payload,
            @RequestHeader(value = "X-Signature", required = false) String signature) {
        // Mock: just log — real implementation verifies HMAC signature from gateway
        return ResponseEntity.ok(ApiResponse.noContent("Webhook received"));
    }
}
