package com.github.haenaryn.inventory.domain.exception;

public class StockNotFoundException extends RuntimeException {

    public StockNotFoundException(Long productOptionId) {
        super("존재하지 않는 재고다 — 상품 옵션 ID: " + productOptionId);
    }
}
