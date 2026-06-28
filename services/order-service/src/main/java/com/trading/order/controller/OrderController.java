package com.trading.order.controller;

import com.trading.order.entity.Order;
import com.trading.order.service.OrderService;
import com.trading.shared.enums.OrderSide;
import com.trading.shared.enums.OrderType;
import com.trading.shared.enums.ProductType;
import com.trading.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Place, cancel, and view orders")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Place a new order (MARKET or LIMIT)")
    @PostMapping
    public ResponseEntity<ApiResponse<Order>> placeOrder(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody PlaceOrderRequest req) {
        Order order = orderService.placeOrder(UUID.fromString(userId),
                new OrderService.PlaceOrderRequest(
                        req.instrumentId(), req.symbol(), req.orderType(),
                        req.side(), req.productType(), req.quantity(),
                        req.price(), req.stopPrice()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(order, "Order placed"));
    }

    @Operation(summary = "Cancel an open order")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Order>> cancelOrder(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(
                orderService.cancelOrder(UUID.fromString(userId), id), "Order cancelled"));
    }

    @Operation(summary = "Get order history (paginated)")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<Order>>> getOrders(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                orderService.getOrders(UUID.fromString(userId), PageRequest.of(page, size))));
    }

    @Operation(summary = "Get a single order by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Order>> getOrder(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(
                orderService.getOrder(UUID.fromString(userId), id)));
    }

    @Operation(summary = "Get all open orders")
    @GetMapping("/open")
    public ResponseEntity<ApiResponse<List<Order>>> getOpenOrders(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.ok(
                orderService.getOpenOrders(UUID.fromString(userId))));
    }

    record PlaceOrderRequest(
            @NotNull UUID instrumentId,
            @NotBlank String symbol,
            @NotNull OrderType orderType,
            @NotNull OrderSide side,
            @NotNull ProductType productType,
            @NotNull @DecimalMin("0.001") BigDecimal quantity,
            BigDecimal price,
            BigDecimal stopPrice
    ) {}
}
