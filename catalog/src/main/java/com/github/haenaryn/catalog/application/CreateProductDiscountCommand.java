package com.github.haenaryn.catalog.application;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateProductDiscountCommand(
    Long productId, String discountType, BigDecimal discountValue, Instant startsAt, Instant endsAt) {
}
