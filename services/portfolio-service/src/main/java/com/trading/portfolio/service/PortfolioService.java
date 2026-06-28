package com.trading.portfolio.service;

import com.trading.portfolio.entity.Holding;
import com.trading.portfolio.repository.HoldingRepository;
import com.trading.shared.enums.OrderSide;
import com.trading.shared.events.TradeExecutedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final HoldingRepository holdingRepository;

    @Transactional(readOnly = true)
    public List<Holding> getHoldings(UUID userId) {
        return holdingRepository.findByUserIdAndQuantityGreaterThan(userId, BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public Holding getHolding(UUID userId, UUID instrumentId) {
        return holdingRepository.findByUserIdAndInstrumentId(userId, instrumentId)
                .orElse(null);
    }

    /**
     * Update holdings whenever a trade is executed.
     * BUY  → increase quantity, recalculate average buy price
     * SELL → decrease quantity, compute realized P&L
     */
    @KafkaListener(topics = "trade.executions", groupId = "portfolio-service-trades")
    @Transactional
    public void onTradeExecuted(TradeExecutedEvent event) {
        Holding holding = holdingRepository
                .findByUserIdAndInstrumentId(event.getUserId(), event.getInstrumentId())
                .orElse(Holding.builder()
                        .userId(event.getUserId())
                        .instrumentId(event.getInstrumentId())
                        .symbol(event.getSymbol())
                        .build());

        if (event.getSide() == OrderSide.BUY) {
            applyBuy(holding, event.getQuantity(), event.getPrice(), event.getFee());
        } else {
            applySell(holding, event.getQuantity(), event.getPrice(), event.getFee());
        }

        holdingRepository.save(holding);
        log.info("Holdings updated for user {} symbol {} qty={} avgPrice={}",
                event.getUserId(), event.getSymbol(),
                holding.getQuantity(), holding.getAverageBuyPrice());
    }

    private void applyBuy(Holding h, BigDecimal qty, BigDecimal price, BigDecimal fee) {
        BigDecimal newTotalCost = h.getTotalInvested()
                .add(qty.multiply(price))
                .add(fee);
        BigDecimal newQty = h.getQuantity().add(qty);

        h.setQuantity(newQty);
        h.setTotalInvested(newTotalCost);
        h.setAverageBuyPrice(newQty.compareTo(BigDecimal.ZERO) > 0
                ? newTotalCost.divide(newQty, 8, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
    }

    private void applySell(Holding h, BigDecimal qty, BigDecimal price, BigDecimal fee) {
        BigDecimal proceeds = qty.multiply(price).subtract(fee);
        BigDecimal costBasis = qty.multiply(h.getAverageBuyPrice());
        BigDecimal pnl = proceeds.subtract(costBasis);

        h.setQuantity(h.getQuantity().subtract(qty).max(BigDecimal.ZERO));
        h.setTotalInvested(h.getTotalInvested().subtract(costBasis).max(BigDecimal.ZERO));
        h.setRealizedPnl(h.getRealizedPnl().add(pnl));
    }
}
