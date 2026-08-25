package com.github.haenaryn.inventory.application;

import com.github.haenaryn.inventory.domain.StockReservationStatus;

public record ReserveStockResult(Long reservationId, Long stockId, StockReservationStatus status) {
}
