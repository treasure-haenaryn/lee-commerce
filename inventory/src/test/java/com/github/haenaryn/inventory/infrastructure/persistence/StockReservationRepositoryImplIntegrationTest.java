package com.github.haenaryn.inventory.infrastructure.persistence;

import com.github.haenaryn.inventory.domain.Stock;
import com.github.haenaryn.inventory.domain.StockRepository;
import com.github.haenaryn.inventory.domain.StockReservation;
import com.github.haenaryn.inventory.domain.StockReservationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class StockReservationRepositoryImplIntegrationTest extends AbstractInventoryIntegrationTest {

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockReservationRepository stockReservationRepository;

    @Test
    void 저장하고_주문_ID와_재고_ID로_조회하면_같은_값을_반환한다() {
        Stock stock = stockRepository.save(Stock.register(1L, 10));
        stockReservationRepository.save(StockReservation.create(stock.getId(), 100L, 3));

        Optional<StockReservation> found =
            stockReservationRepository.findByOrderIdAndStockId(100L, stock.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getQuantity()).isEqualTo(3);
    }

    @Test
    void 같은_주문이_같은_재고를_두_번_예약하면_유일성_제약_위반으로_실패한다() {
        Stock stock = stockRepository.save(Stock.register(2L, 10));
        stockReservationRepository.save(StockReservation.create(stock.getId(), 200L, 2));

        assertThatThrownBy(() ->
            stockReservationRepository.save(StockReservation.create(stock.getId(), 200L, 1)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}
