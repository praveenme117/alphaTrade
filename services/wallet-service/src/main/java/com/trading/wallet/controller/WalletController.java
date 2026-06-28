package com.trading.wallet.controller;

import com.trading.shared.response.ApiResponse;
import com.trading.wallet.entity.Wallet;
import com.trading.wallet.entity.WalletLedger;
import com.trading.wallet.repository.WalletLedgerRepository;
import com.trading.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
@Tag(name = "Wallet", description = "Balance, fund management, transaction ledger")
public class WalletController {

    private final WalletService walletService;
    private final WalletLedgerRepository ledgerRepository;

    @Operation(summary = "Get all wallets for the authenticated user")
    @GetMapping
    public ResponseEntity<ApiResponse<List<Wallet>>> getWallets(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.ok(walletService.getWallets(UUID.fromString(userId))));
    }

    @Operation(summary = "Get wallet by currency (INR or USDT)")
    @GetMapping("/{currency}")
    public ResponseEntity<ApiResponse<Wallet>> getWallet(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String currency) {
        return ResponseEntity.ok(ApiResponse.ok(
                walletService.getWallet(UUID.fromString(userId), currency.toUpperCase())));
    }

    @Operation(summary = "Get paginated transaction ledger")
    @GetMapping("/ledger")
    public ResponseEntity<ApiResponse<Page<WalletLedger>>> getLedger(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<WalletLedger> ledger = ledgerRepository.findByUserIdOrderByCreatedAtDesc(
                UUID.fromString(userId), PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(ledger));
    }

    /**
     * Internal endpoint — called by order-service via Feign.
     * Not exposed through gateway to end-users directly.
     */
    @Operation(summary = "Lock funds for an open order (internal)")
    @PostMapping("/lock")
    public ResponseEntity<ApiResponse<?>> lockFunds(
            @RequestBody LockFundsRequest request) {
        walletService.lockFunds(request.userId(), request.currency(),
                request.amount(), request.orderId());
        return ResponseEntity.ok(ApiResponse.noContent("Funds locked"));
    }

    @Operation(summary = "Unlock funds for a cancelled order (internal)")
    @PostMapping("/unlock")
    public ResponseEntity<ApiResponse<?>> unlockFunds(
            @RequestBody LockFundsRequest request) {
        walletService.unlockFunds(request.userId(), request.currency(),
                request.amount(), request.orderId());
        return ResponseEntity.ok(ApiResponse.noContent("Funds unlocked"));
    }

    record LockFundsRequest(UUID userId, String currency, java.math.BigDecimal amount, UUID orderId) {}
}
