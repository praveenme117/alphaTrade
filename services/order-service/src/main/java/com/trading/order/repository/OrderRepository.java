package com.trading.order.repository;

import com.trading.order.entity.Order;
import com.trading.shared.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<Order> findByUserIdAndSymbolOrderByCreatedAtDesc(UUID userId, String symbol);

    List<Order> findByUserIdAndStatus(UUID userId, OrderStatus status);
}
