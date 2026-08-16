package com.github.haenaryn.catalog.application;

public record DeactivateProductOptionCommand(Long productId, String skuCode) {
}
