package com.github.haenaryn.inventory.application;

public record ConfirmStockCommand(Long productOptionId, Long orderId) {
}
