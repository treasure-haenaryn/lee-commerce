package com.github.haenaryn.catalog.domain;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(Long id);

    List<Product> findAllByCategoryId(Long categoryId);

    // 항상 존재가 보장된 부모 행을 잠가 그 상품에 딸린 다른 Aggregate(ProductDiscount 등)
    // 생성 시 경쟁 조건을 막는 용도다 — 잠글 대상 자식 행이 아직 없는 phantom 케이스도
    // 함께 막을 수 있다.
    Optional<Product> findByIdForUpdate(Long id);
}
