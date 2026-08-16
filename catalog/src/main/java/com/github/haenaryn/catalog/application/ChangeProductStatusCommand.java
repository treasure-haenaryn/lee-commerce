package com.github.haenaryn.catalog.application;

public record ChangeProductStatusCommand(Long productId, String status) {
}
