package com.github.haenaryn.catalog.application;

import java.math.BigDecimal;

public record ProductSearchResultItem(
    Long productId, String name, Long categoryId, BigDecimal basePriceAmount,
    String basePriceCurrency, String status) {
}
