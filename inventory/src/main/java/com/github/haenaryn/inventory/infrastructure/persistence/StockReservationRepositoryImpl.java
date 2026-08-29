package com.github.haenaryn.inventory.infrastructure.persistence;

import com.github.haenaryn.inventory.domain.StockReservation;
import com.github.haenaryn.inventory.domain.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
class StockReservationRepositoryImpl implements StockReservationRepository {

    private final StockReservationJpaRepository jpaRepository;

    @Override
    public StockReservation save(StockReservation stockReservation) {
        return jpaRepository.save(stockReservation);
    }

    @Override
    public Optional<StockReservation> findByOrderIdAndStockId(Long orderId, Long stockId) {
        return jpaRepository.findByOrderIdAndStockId(orderId, stockId);
    }
}
