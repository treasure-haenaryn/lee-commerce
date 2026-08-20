package com.github.haenaryn.catalog.interfaces;

import java.math.BigDecimal;

public record SearchProductItemResponse(
    Long productId, String name, Long categoryId, BigDecimal basePriceAmount,
    String basePriceCurrency, String status) {
}
