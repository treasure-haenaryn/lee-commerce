package com.github.haenaryn.inventory.domain;

import java.util.Optional;

public interface StockReservationRepository {

    StockReservation save(StockReservation stockReservation);

    Optional<StockReservation> findByOrderIdAndStockId(Long orderId, Long stockId);
}
