package com.github.haenaryn.catalog.application;

import java.math.BigDecimal;

// status는 SearchProductsService만 채운다 — 검색 결과를 판매중 상품으로 제한하는 건
// 검색 UseCase의 정책이라, 호출자가 임의로 다른 상태를 지정할 수 없게 한다.
public record SearchProductsQuery(
    String name, Long categoryId, BigDecimal minPriceAmount, BigDecimal maxPriceAmount,
    String status, int page, int size) {
}
