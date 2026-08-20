package com.github.haenaryn.inventory.domain;

import java.util.Optional;

public interface StockRepository {

    Stock save(Stock stock);

    Optional<Stock> findById(Long id);

    Optional<Stock> findByProductOptionId(Long productOptionId);
}
