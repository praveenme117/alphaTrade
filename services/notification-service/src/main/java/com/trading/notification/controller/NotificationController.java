package com.trading.notification.controller;

import com.trading.notification.entity.Notification;
import com.trading.notification.service.NotificationService;
import com.trading.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app trade and payment notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Get paginated notifications")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<Notification>>> getNotifications(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                notificationService.getNotifications(UUID.fromString(userId), PageRequest.of(page, size))));
    }

    @Operation(summary = "Get unread notification count")
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> unreadCount(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.ok(
                notificationService.countUnread(UUID.fromString(userId))));
    }

    @Operation(summary = "Mark a single notification as read")
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<?>> markRead(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID id) {
        notificationService.markRead(UUID.fromString(userId), id);
        return ResponseEntity.ok(ApiResponse.noContent("Marked as read"));
    }

    @Operation(summary = "Mark all notifications as read")
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<?>> markAllRead(
            @RequestHeader("X-User-Id") String userId) {
        notificationService.markAllRead(UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.noContent("All notifications marked as read"));
    }
}
