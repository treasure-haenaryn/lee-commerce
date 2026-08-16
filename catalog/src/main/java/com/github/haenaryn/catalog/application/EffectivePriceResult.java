package com.github.haenaryn.catalog.application;

import java.math.BigDecimal;

public record EffectivePriceResult(
    Long productId, String skuCode, BigDecimal amount, String currency, boolean discountApplied) {
}
