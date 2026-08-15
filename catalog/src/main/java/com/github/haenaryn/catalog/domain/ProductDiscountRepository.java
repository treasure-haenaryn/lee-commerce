package com.github.haenaryn.catalog.domain;

import java.util.List;
import java.util.Optional;

public interface ProductDiscountRepository {

    ProductDiscount save(ProductDiscount discount);

    Optional<ProductDiscount> findById(Long id);

    List<ProductDiscount> findAllByProductId(Long productId);
}
