package com.github.haenaryn.catalog.domain;

import com.github.haenaryn.common.vo.Money;

// Product 생성/옵션 추가 시 입력값을 감싼다. priceOverride가 null이면 상품 기본가를 따른다.
public record ProductOptionSpec(String skuCode, String size, String color, Money priceOverride) {
}
