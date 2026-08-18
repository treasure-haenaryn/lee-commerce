package com.github.haenaryn.catalog.application;

public interface ProductSearchPort {

    SearchProductsResult search(SearchProductsQuery query);
}
