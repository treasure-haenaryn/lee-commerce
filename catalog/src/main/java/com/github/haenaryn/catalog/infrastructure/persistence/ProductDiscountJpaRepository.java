package com.github.haenaryn.catalog.infrastructure.persistence;

import com.github.haenaryn.catalog.domain.ProductDiscount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ProductDiscountJpaRepository extends JpaRepository<ProductDiscount, Long> {

    List<ProductDiscount> findAllByProductId(Long productId);
}
