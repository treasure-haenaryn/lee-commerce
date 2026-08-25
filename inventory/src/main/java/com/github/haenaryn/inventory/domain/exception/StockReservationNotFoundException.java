package com.github.haenaryn.inventory.domain.exception;

public class StockReservationNotFoundException extends RuntimeException {

    public StockReservationNotFoundException(Long orderId, Long stockId) {
        super("존재하지 않는 재고 예약이다 — 주문 ID: %d, 재고 ID: %d".formatted(orderId, stockId));
    }
}
