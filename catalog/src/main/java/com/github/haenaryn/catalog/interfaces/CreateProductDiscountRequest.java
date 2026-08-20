package com.github.haenaryn.catalog.interfaces;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateProductDiscountRequest(
    @NotBlank String discountType, @NotNull BigDecimal discountValue, Instant startsAt, Instant endsAt) {
}
