package com.github.haenaryn.inventory.infrastructure.persistence;

import com.github.haenaryn.inventory.domain.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface StockReservationJpaRepository extends JpaRepository<StockReservation, Long> {

    Optional<StockReservation> findByOrderIdAndStockId(Long orderId, Long stockId);
}
