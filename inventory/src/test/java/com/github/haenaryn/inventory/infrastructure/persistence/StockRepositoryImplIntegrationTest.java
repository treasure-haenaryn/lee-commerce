package com.github.haenaryn.inventory.infrastructure.persistence;

import com.github.haenaryn.inventory.domain.Stock;
import com.github.haenaryn.inventory.domain.StockRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class StockRepositoryImplIntegrationTest extends AbstractInventoryIntegrationTest {

    @Autowired
    private StockRepository stockRepository;

    @Test
    void 저장하고_ID로_조회하면_같은_값을_반환한다() {
        Stock stock = stockRepository.save(Stock.register(1L, 10));

        Optional<Stock> found = stockRepository.findById(stock.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getProductOptionId()).isEqualTo(1L);
        assertThat(found.get().getAvailableQuantity()).isEqualTo(10);
        assertThat(found.get().getReservedQuantity()).isEqualTo(0);
    }

    @Test
    void 상품_옵션_ID로_조회할_수_있다() {
        stockRepository.save(Stock.register(2L, 5));

        Optional<Stock> found = stockRepository.findByProductOptionId(2L);

        assertThat(found).isPresent();
        assertThat(found.get().getProductOptionId()).isEqualTo(2L);
    }

    @Test
    void 존재하지_않는_상품_옵션_ID로_조회하면_빈_값을_반환한다() {
        Optional<Stock> found = stockRepository.findByProductOptionId(999L);

        assertThat(found).isEmpty();
    }
}
