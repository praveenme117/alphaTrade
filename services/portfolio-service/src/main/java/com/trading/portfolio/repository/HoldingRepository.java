package com.trading.portfolio.repository;

import com.trading.portfolio.entity.Holding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HoldingRepository extends JpaRepository<Holding, UUID> {

    List<Holding> findByUserIdAndQuantityGreaterThan(UUID userId, BigDecimal minQuantity);

    Optional<Holding> findByUserIdAndInstrumentId(UUID userId, UUID instrumentId);
}
