package com.github.haenaryn.catalog.application;

import java.math.BigDecimal;

public record SearchProductsCommand(
    String name, Long categoryId, BigDecimal minPriceAmount, BigDecimal maxPriceAmount, int page, int size) {
}
