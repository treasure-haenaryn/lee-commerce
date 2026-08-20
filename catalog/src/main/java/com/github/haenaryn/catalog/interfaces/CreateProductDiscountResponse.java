package com.github.haenaryn.catalog.interfaces;

import java.math.BigDecimal;

public record CreateProductDiscountResponse(Long discountId, Long productId, String discountType, BigDecimal discountValue) {
}
