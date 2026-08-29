package com.github.haenaryn.inventory.infrastructure.persistence;

import com.github.haenaryn.inventory.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface StockJpaRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByProductOptionId(Long productOptionId);
}
