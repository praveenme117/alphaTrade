package com.trading.order.exception;

import com.trading.shared.exception.TradingException;
import com.trading.shared.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TradingException.class)
    public ResponseEntity<ApiResponse<Void>> handle(TradingException ex) {
        log.warn("TradingException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.valueOf(ex.getHttpStatus()))
                .body(ApiResponse.error(ex.getMessage(), ex.getErrorCode()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        log.error("Unhandled error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Internal error", "INTERNAL_ERROR"));
    }
}
