package com.github.haenaryn.catalog.interfaces;

import java.util.List;

public record RegisterProductResponse(Long productId, String name, String status, List<String> skuCodes) {
}
