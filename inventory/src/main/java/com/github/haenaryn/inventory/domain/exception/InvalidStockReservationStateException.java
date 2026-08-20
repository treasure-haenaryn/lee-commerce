package com.github.haenaryn.inventory.domain.exception;

import com.github.haenaryn.inventory.domain.StockReservationStatus;

public class InvalidStockReservationStateException extends RuntimeException {

    public InvalidStockReservationStateException(Long reservationId, StockReservationStatus currentStatus) {
        super("예약 상태에서만 처리할 수 있다 — 예약 ID: %d, 현재 상태: %s"
            .formatted(reservationId, currentStatus));
    }
}
