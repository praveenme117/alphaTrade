package com.trading.order.client;

import com.trading.order.client.dto.LockFundsRequest;
import com.trading.shared.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * OpenFeign client for synchronous wallet fund locking.
 * Used during order placement (critical path — must succeed before order is OPEN).
 */
@FeignClient(name = "wallet-service", url = "${services.wallet-service.url}")
public interface WalletFeignClient {

    @PostMapping("/api/v1/wallet/lock")
    ApiResponse<Void> lockFunds(@RequestBody LockFundsRequest request);

    @PostMapping("/api/v1/wallet/unlock")
    ApiResponse<Void> unlockFunds(@RequestBody LockFundsRequest request);
}
