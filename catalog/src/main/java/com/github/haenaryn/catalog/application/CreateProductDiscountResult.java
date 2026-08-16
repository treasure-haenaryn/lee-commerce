package com.github.haenaryn.catalog.application;

import java.math.BigDecimal;

public record CreateProductDiscountResult(Long discountId, Long productId, String discountType, BigDecimal discountValue) {
}
