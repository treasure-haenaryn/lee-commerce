package com.github.haenaryn.inventory.domain.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(Long productOptionId, int requested, int available) {
        super("재고가 부족하다 — 상품 옵션 ID: %d, 요청 수량: %d, 가용 수량: %d"
            .formatted(productOptionId, requested, available));
    }
}
