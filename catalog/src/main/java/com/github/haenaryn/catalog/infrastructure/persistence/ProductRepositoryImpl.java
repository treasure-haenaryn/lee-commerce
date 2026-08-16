package com.github.haenaryn.catalog.infrastructure.persistence;

import com.github.haenaryn.catalog.domain.Product;
import com.github.haenaryn.catalog.domain.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository jpaRepository;

    @Override
    public Product save(Product product) {
        return jpaRepository.save(product);
    }

    @Override
    public Optional<Product> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Product> findAllByCategoryId(Long categoryId) {
        return jpaRepository.findAllByCategoryId(categoryId);
    }

    @Override
    public Optional<Product> findByIdForUpdate(Long id) {
        return jpaRepository.findByIdForUpdate(id);
    }
}
