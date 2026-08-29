package com.github.haenaryn.inventory.infrastructure.persistence;

import com.github.haenaryn.inventory.domain.Stock;
import com.github.haenaryn.inventory.domain.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
class StockRepositoryImpl implements StockRepository {

    private final StockJpaRepository jpaRepository;

    @Override
    public Stock save(Stock stock) {
        return jpaRepository.save(stock);
    }

    @Override
    public Optional<Stock> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Stock> findByProductOptionId(Long productOptionId) {
        return jpaRepository.findByProductOptionId(productOptionId);
    }
}
