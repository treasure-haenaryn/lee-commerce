package com.github.haenaryn.catalog.application;

import java.util.List;

public record RegisterProductResult(Long productId, String name, String status, List<String> skuCodes) {
}
