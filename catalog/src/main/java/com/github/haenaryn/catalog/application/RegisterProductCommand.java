package com.github.haenaryn.catalog.application;

import java.math.BigDecimal;
import java.util.List;

public record RegisterProductCommand(
    Long categoryId,
    String name,
    String description,
    BigDecimal basePriceAmount,
    String basePriceCurrency,
    List<ProductOptionSpecCommand> options) {
}
