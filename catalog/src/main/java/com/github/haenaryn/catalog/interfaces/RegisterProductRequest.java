package com.github.haenaryn.catalog.interfaces;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record RegisterProductRequest(
    @NotNull Long categoryId,
    @NotBlank String name,
    String description,
    @NotNull BigDecimal basePriceAmount,
    @NotBlank String basePriceCurrency,
    @NotEmpty List<@Valid ProductOptionRequest> options) {
}
