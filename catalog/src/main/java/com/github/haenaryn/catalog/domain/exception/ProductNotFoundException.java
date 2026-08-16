package com.github.haenaryn.catalog.domain.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long productId) {
        super("존재하지 않는 상품이다: " + productId);
    }
}
