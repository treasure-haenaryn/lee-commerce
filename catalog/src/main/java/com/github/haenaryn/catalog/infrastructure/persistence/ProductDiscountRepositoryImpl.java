package com.github.haenaryn.catalog.infrastructure.persistence;

import com.github.haenaryn.catalog.domain.ProductDiscount;
import com.github.haenaryn.catalog.domain.ProductDiscountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
class ProductDiscountRepositoryImpl implements ProductDiscountRepository {

    private final ProductDiscountJpaRepository jpaRepository;

    @Override
    public ProductDiscount save(ProductDiscount discount) {
        return jpaRepository.save(discount);
    }

    @Override
    public Optional<ProductDiscount> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<ProductDiscount> findAllByProductId(Long productId) {
        return jpaRepository.findAllByProductId(productId);
    }
}
