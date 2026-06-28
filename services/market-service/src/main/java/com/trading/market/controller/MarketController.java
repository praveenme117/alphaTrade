package com.trading.market.controller;

import com.trading.market.dto.InstrumentDto;
import com.trading.market.dto.QuoteDto;
import com.trading.market.service.MarketService;
import com.trading.shared.enums.InstrumentType;
import com.trading.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/market")
@RequiredArgsConstructor
@Tag(name = "Market Data", description = "Instruments, live quotes, search")
public class MarketController {

    private final MarketService marketService;

    // ─── Instruments ─────────────────────────────────────────────

    @Operation(summary = "List all active instruments")
    @GetMapping("/instruments")
    public ResponseEntity<ApiResponse<List<InstrumentDto>>> getAllInstruments(
            @Parameter(description = "Filter by type: STOCK, CRYPTO, ETF")
            @RequestParam(required = false) InstrumentType type) {
        List<InstrumentDto> instruments = type != null
                ? marketService.getInstrumentsByType(type)
                : marketService.getAllInstruments();
        return ResponseEntity.ok(ApiResponse.ok(instruments));
    }

    @Operation(summary = "Get instrument by ID")
    @GetMapping("/instruments/{id}")
    public ResponseEntity<ApiResponse<InstrumentDto>> getInstrumentById(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(marketService.getInstrumentById(id)));
    }

    @Operation(summary = "Get instrument by symbol (e.g., BTC, RELIANCE)")
    @GetMapping("/instruments/symbol/{symbol}")
    public ResponseEntity<ApiResponse<InstrumentDto>> getInstrumentBySymbol(
            @PathVariable String symbol) {
        return ResponseEntity.ok(ApiResponse.ok(marketService.getInstrumentBySymbol(symbol)));
    }

    @Operation(summary = "Search instruments by symbol or name")
    @GetMapping("/instruments/search")
    public ResponseEntity<ApiResponse<List<InstrumentDto>>> search(
            @Parameter(description = "Search query (partial match on symbol or name)")
            @RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.ok(marketService.search(q)));
    }

    // ─── Quotes (live prices) ────────────────────────────────────

    @Operation(summary = "Get live quote for a symbol")
    @GetMapping("/quotes/{symbol}")
    public ResponseEntity<ApiResponse<QuoteDto>> getQuote(
            @PathVariable String symbol) {
        return ResponseEntity.ok(ApiResponse.ok(marketService.getQuote(symbol)));
    }

    @Operation(summary = "Get live quotes for multiple symbols")
    @GetMapping("/quotes")
    public ResponseEntity<ApiResponse<List<QuoteDto>>> getQuotes(
            @Parameter(description = "Comma-separated symbols, e.g., BTC,ETH,RELIANCE")
            @RequestParam List<String> symbols) {
        return ResponseEntity.ok(ApiResponse.ok(marketService.getQuotes(symbols)));
    }
}
