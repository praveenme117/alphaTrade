package com.trading.wallet.service;

import com.trading.shared.events.PaymentCompletedEvent;
import com.trading.shared.events.TradeExecutedEvent;
import com.trading.shared.exception.InsufficientFundsException;
import com.trading.shared.exception.ResourceNotFoundException;
import com.trading.wallet.entity.Wallet;
import com.trading.wallet.entity.WalletLedger;
import com.trading.wallet.repository.WalletLedgerRepository;
import com.trading.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletLedgerRepository ledgerRepository;

    // ─── Wallet lifecycle ──────────────────────────────────────

    /**
     * Creates INR + USDT wallets for a new user, seeded with mock balances.
     * Called on user registration (idempotent).
     */
    @Transactional
    public void createWalletsForUser(UUID userId) {
        if (!walletRepository.existsByUserIdAndCurrency(userId, "INR")) {
            Wallet inrWallet = walletRepository.save(Wallet.builder()
                    .userId(userId).currency("INR")
                    .balance(new BigDecimal("100000.00"))   // ₹1,00,000 seed
                    .build());
            recordLedger(inrWallet, "CREDIT", new BigDecimal("100000.00"),
                    new BigDecimal("100000.00"), "Welcome bonus — mock seed balance",
                    null, "MANUAL");
        }
        if (!walletRepository.existsByUserIdAndCurrency(userId, "USDT")) {
            Wallet usdtWallet = walletRepository.save(Wallet.builder()
                    .userId(userId).currency("USDT")
                    .balance(new BigDecimal("1500.00"))     // 1500 USDT seed
                    .build());
            recordLedger(usdtWallet, "CREDIT", new BigDecimal("1500.00"),
                    new BigDecimal("1500.00"), "Welcome bonus — mock USDT seed",
                    null, "MANUAL");
        }
        log.info("Created INR + USDT wallets for user {}", userId);
    }

    // ─── Balance operations ────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Wallet> getWallets(UUID userId) {
        return walletRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Wallet getWallet(UUID userId, String currency) {
        return walletRepository.findByUserIdAndCurrency(userId, currency)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", userId + "/" + currency));
    }

    /**
     * Lock funds for an open order (prevents double-spend).
     */
    @Transactional
    public void lockFunds(UUID userId, String currency, BigDecimal amount, UUID orderId) {
        Wallet wallet = getWalletWithLock(userId, currency);
        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    String.format("Insufficient %s balance. Available: %s, Required: %s",
                            currency, wallet.getAvailableBalance(), amount));
        }
        wallet.setLockedBalance(wallet.getLockedBalance().add(amount));
        walletRepository.save(wallet);
        recordLedger(wallet, "LOCK", amount, wallet.getBalance(),
                "Funds locked for order", orderId, "ORDER");
        log.info("Locked {} {} for user {} (order {})", amount, currency, userId, orderId);
    }

    /**
     * Release locked funds back to available (on order cancel or rejection).
     */
    @Transactional
    public void unlockFunds(UUID userId, String currency, BigDecimal amount, UUID orderId) {
        Wallet wallet = getWalletWithLock(userId, currency);
        BigDecimal newLocked = wallet.getLockedBalance().subtract(amount).max(BigDecimal.ZERO);
        wallet.setLockedBalance(newLocked);
        walletRepository.save(wallet);
        recordLedger(wallet, "UNLOCK", amount, wallet.getBalance(),
                "Funds unlocked — order cancelled/rejected", orderId, "ORDER");
    }

    /**
     * Debit funds for a filled order (moves from locked to deducted).
     */
    @Transactional
    public void debitForTrade(UUID userId, String currency, BigDecimal amount,
                              BigDecimal fee, UUID orderId) {
        Wallet wallet = getWalletWithLock(userId, currency);
        BigDecimal total = amount.add(fee);
        wallet.setBalance(wallet.getBalance().subtract(total));
        wallet.setLockedBalance(wallet.getLockedBalance().subtract(amount).max(BigDecimal.ZERO));
        walletRepository.save(wallet);
        recordLedger(wallet, "DEBIT", total, wallet.getBalance(),
                String.format("Trade debit (amount=%.2f fee=%.4f)", amount.doubleValue(), fee.doubleValue()),
                orderId, "ORDER");
    }

    /**
     * Credit funds from a sell trade or payment deposit.
     */
    @Transactional
    public void credit(UUID userId, String currency, BigDecimal amount,
                       String description, UUID referenceId, String referenceType) {
        Wallet wallet = getWalletWithLock(userId, currency);
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);
        recordLedger(wallet, "CREDIT", amount, wallet.getBalance(),
                description, referenceId, referenceType);
    }

    // ─── Kafka consumers ──────────────────────────────────────

    /**
     * On payment completion → credit wallet.
     */
    @KafkaListener(topics = "payments.completed", groupId = "wallet-service-payments")
    @Transactional
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        credit(event.getUserId(), event.getCurrency(), event.getAmount(),
                "Deposit via " + event.getGateway(), event.getPaymentOrderId(), "PAYMENT");
        log.info("Credited {} {} to user {} (payment {})",
                event.getAmount(), event.getCurrency(), event.getUserId(), event.getPaymentOrderId());
    }

    /**
     * On trade execution → debit buyer, credit seller proceeds.
     * order-service publishes TradeExecutedEvent after each fill.
     */
    @KafkaListener(topics = "trade.executions", groupId = "wallet-service-trades")
    @Transactional
    public void onTradeExecuted(TradeExecutedEvent event) {
        String currency = event.getSymbol().endsWith("USDT") ? "USDT" : "INR";
        if ("BUY".equals(event.getSide().name())) {
            debitForTrade(event.getUserId(), currency,
                    event.getTotalValue(), event.getFee(), event.getOrderId());
        } else {
            // SELL — credit proceeds minus fee
            BigDecimal proceeds = event.getTotalValue().subtract(event.getFee());
            credit(event.getUserId(), currency, proceeds,
                    "Trade proceeds: SELL " + event.getSymbol(),
                    event.getOrderId(), "ORDER");
        }
    }

    // ─── Helpers ──────────────────────────────────────────────

    private Wallet getWalletWithLock(UUID userId, String currency) {
        return walletRepository.findByUserIdAndCurrencyForUpdate(userId, currency)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", userId + "/" + currency));
    }

    private void recordLedger(Wallet wallet, String type, BigDecimal amount,
                               BigDecimal balanceAfter, String description,
                               UUID referenceId, String referenceType) {
        ledgerRepository.save(WalletLedger.builder()
                .wallet(wallet)
                .type(type)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .description(description)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .build());
    }
}
