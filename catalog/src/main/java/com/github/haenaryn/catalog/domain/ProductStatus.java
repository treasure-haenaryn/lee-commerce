package com.github.haenaryn.catalog.domain;

// 할인 여부는 여기 포함하지 않는다 — 할인은 ProductDiscount로 별도 관리되는 독립된 축이다.
public enum ProductStatus {
    ON_SALE,
    SOLD_OUT,
    HIDDEN,
    DISCONTINUED
}
