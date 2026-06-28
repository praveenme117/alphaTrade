package com.trading.market.repository;

import com.trading.market.entity.Instrument;
import com.trading.shared.enums.InstrumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstrumentRepository extends JpaRepository<Instrument, UUID> {

    Optional<Instrument> findBySymbol(String symbol);

    List<Instrument> findByTypeAndActiveTrue(InstrumentType type);

    List<Instrument> findAllByActiveTrue();

    @Query("""
        SELECT i FROM Instrument i
        WHERE i.active = true
          AND (UPPER(i.symbol) LIKE UPPER(CONCAT('%', :query, '%'))
            OR UPPER(i.name)   LIKE UPPER(CONCAT('%', :query, '%')))
        ORDER BY i.symbol
        """)
    List<Instrument> searchBySymbolOrName(String query);
}
