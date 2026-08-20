package com.github.haenaryn.catalog.interfaces;

import java.util.List;

public record SearchProductsResponse(List<SearchProductItemResponse> items, long totalCount, int page, int size) {
}
