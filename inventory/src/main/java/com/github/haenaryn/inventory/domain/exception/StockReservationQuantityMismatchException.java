package com.github.haenaryn.inventory.domain.exception;

public class StockReservationQuantityMismatchException extends RuntimeException {

    public StockReservationQuantityMismatchException(Long productOptionId, int requested, int reserved) {
        super("예약 수량을 초과하는 처리 요청이다 — 상품 옵션 ID: %d, 요청 수량: %d, 예약 수량: %d"
            .formatted(productOptionId, requested, reserved));
    }
}
