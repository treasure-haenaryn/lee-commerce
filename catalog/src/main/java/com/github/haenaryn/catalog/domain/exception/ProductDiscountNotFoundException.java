package com.github.haenaryn.catalog.domain.exception;

public class ProductDiscountNotFoundException extends RuntimeException {

    public ProductDiscountNotFoundException(Long discountId) {
        super("존재하지 않는 할인이다: " + discountId);
    }
}
