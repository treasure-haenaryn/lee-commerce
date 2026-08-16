package com.github.haenaryn.catalog.application;

import java.math.BigDecimal;

public record AddProductOptionCommand(
    Long productId, String skuCode, String size, String color,
    BigDecimal priceOverrideAmount, String priceOverrideCurrency) {
}
