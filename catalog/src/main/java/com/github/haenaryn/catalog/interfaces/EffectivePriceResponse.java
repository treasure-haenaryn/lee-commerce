package com.github.haenaryn.catalog.interfaces;

import java.math.BigDecimal;

public record EffectivePriceResponse(
    Long productId, String skuCode, BigDecimal amount, String currency, boolean discountApplied) {
}
