package com.github.haenaryn.inventory.application;

public record ReserveStockCommand(Long productOptionId, Long orderId, int quantity) {
}
