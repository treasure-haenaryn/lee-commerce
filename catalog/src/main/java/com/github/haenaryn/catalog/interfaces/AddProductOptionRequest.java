package com.github.haenaryn.catalog.interfaces;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record AddProductOptionRequest(
    @NotBlank String skuCode, String size, String color,
    BigDecimal priceOverrideAmount, String priceOverrideCurrency) {
}
