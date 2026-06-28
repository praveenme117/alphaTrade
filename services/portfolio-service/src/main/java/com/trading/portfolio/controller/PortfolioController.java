package com.trading.portfolio.controller;

import com.trading.portfolio.entity.Holding;
import com.trading.portfolio.service.PortfolioService;
import com.trading.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/portfolio")
@RequiredArgsConstructor
@Tag(name = "Portfolio", description = "Holdings, P&L, position summary")
public class PortfolioController {

    private final PortfolioService portfolioService;

    @Operation(summary = "Get all current holdings (quantity > 0)")
    @GetMapping("/holdings")
    public ResponseEntity<ApiResponse<List<Holding>>> getHoldings(
            @RequestHeader("X-User-Id") String userId) {
        List<Holding> holdings = portfolioService.getHoldings(UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.ok(holdings));
    }

    @Operation(summary = "Get holding for a specific instrument")
    @GetMapping("/holdings/{instrumentId}")
    public ResponseEntity<ApiResponse<Holding>> getHolding(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID instrumentId) {
        Holding holding = portfolioService.getHolding(UUID.fromString(userId), instrumentId);
        return ResponseEntity.ok(ApiResponse.ok(holding));
    }

    @Operation(summary = "Get portfolio summary (total invested, total P&L)")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<PortfolioSummary>> getSummary(
            @RequestHeader("X-User-Id") String userId) {
        List<Holding> holdings = portfolioService.getHoldings(UUID.fromString(userId));
        BigDecimal totalInvested = holdings.stream()
                .map(Holding::getTotalInvested)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRealizedPnl = holdings.stream()
                .map(Holding::getRealizedPnl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return ResponseEntity.ok(ApiResponse.ok(new PortfolioSummary(
                holdings.size(), totalInvested, totalRealizedPnl)));
    }

    record PortfolioSummary(int holdingsCount, BigDecimal totalInvested, BigDecimal totalRealizedPnl) {}
}
