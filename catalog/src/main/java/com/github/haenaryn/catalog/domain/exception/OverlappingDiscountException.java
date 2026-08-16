package com.github.haenaryn.catalog.domain.exception;

public class OverlappingDiscountException extends RuntimeException {

    public OverlappingDiscountException(Long productId) {
        super("해당 상품에 이미 기간이 겹치는 활성 할인이 있다: " + productId);
    }
}
