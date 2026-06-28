package com.trading.shared.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Unified API response wrapper used across all microservices.
 *
 * Factory methods (unambiguous):
 *   ok(data)               — data only
 *   ok(data, message)      — with message  [NOTE: data is first, always typed]
 *   noContent(message)     — success with message, no data (replaces ok(null,"msg"))
 *   error(message, code)   — failure
 *
 * @param <T> the data payload type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private String errorCode;

    @Builder.Default
    private Instant timestamp = Instant.now();

    /** Success with data only */
    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    /** Success with data + message */
    public static <T> ApiResponse<T> ok(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /** Success with message, no data (for void operations: cancel, mark-read, etc.) */
    public static ApiResponse<Void> noContent(String message) {
        return ApiResponse.<Void>builder()
                .success(true)
                .message(message)
                .build();
    }

    /** Failure */
    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .build();
    }
}
