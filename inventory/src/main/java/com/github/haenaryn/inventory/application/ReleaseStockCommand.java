package com.github.haenaryn.inventory.application;

public record ReleaseStockCommand(Long productOptionId, Long orderId) {
}
