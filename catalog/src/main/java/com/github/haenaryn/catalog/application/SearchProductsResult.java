package com.github.haenaryn.catalog.application;

import java.util.List;

public record SearchProductsResult(List<ProductSearchResultItem> items, long totalCount, int page, int size) {
}
