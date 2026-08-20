package com.github.haenaryn.inventory.domain;

import com.github.haenaryn.inventory.domain.exception.InsufficientStockException;
import com.github.haenaryn.inventory.domain.exception.StockReservationQuantityMismatchException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockTest {

    @Test
    void 상품_옵션_ID가_없으면_등록에_실패한다() {
        assertThatThrownBy(() -> Stock.register(null, 10))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 초기_수량이_음수면_등록에_실패한다() {
        assertThatThrownBy(() -> Stock.register(1L, -1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 등록하면_가용_수량은_초기_수량_예약_수량은_0이다() {
        Stock stock = Stock.register(1L, 10);

        assertThat(stock.getAvailableQuantity()).isEqualTo(10);
        assertThat(stock.getReservedQuantity()).isEqualTo(0);
    }

    @Test
    void 예약하면_가용_수량이_줄고_예약_수량이_는다() {
        Stock stock = Stock.register(1L, 10);

        stock.reserve(3);

        assertThat(stock.getAvailableQuantity()).isEqualTo(7);
        assertThat(stock.getReservedQuantity()).isEqualTo(3);
    }

    @Test
    void 가용_수량보다_많이_예약하면_예외() {
        Stock stock = Stock.register(1L, 5);

        assertThatThrownBy(() -> stock.reserve(6))
            .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void 예약_수량이_0이하면_예외() {
        Stock stock = Stock.register(1L, 10);

        assertThatThrownBy(() -> stock.reserve(0))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 여러번_예약해도_수량이_누적된다() {
        Stock stock = Stock.register(1L, 10);

        stock.reserve(3);
        stock.reserve(2);

        assertThat(stock.getAvailableQuantity()).isEqualTo(5);
        assertThat(stock.getReservedQuantity()).isEqualTo(5);
    }

    @Test
    void 확정하면_예약_수량만_줄고_가용_수량은_그대로다() {
        Stock stock = Stock.register(1L, 10);
        stock.reserve(4);

        stock.confirmReservation(4);

        assertThat(stock.getAvailableQuantity()).isEqualTo(6);
        assertThat(stock.getReservedQuantity()).isEqualTo(0);
    }

    @Test
    void 예약_수량보다_많이_확정하면_예외() {
        Stock stock = Stock.register(1L, 10);
        stock.reserve(2);

        assertThatThrownBy(() -> stock.confirmReservation(3))
            .isInstanceOf(StockReservationQuantityMismatchException.class);
    }

    @Test
    void 해제하면_예약_수량이_줄고_가용_수량으로_되돌아간다() {
        Stock stock = Stock.register(1L, 10);
        stock.reserve(4);

        stock.releaseReservation(4);

        assertThat(stock.getAvailableQuantity()).isEqualTo(10);
        assertThat(stock.getReservedQuantity()).isEqualTo(0);
    }

    @Test
    void 예약_수량보다_많이_해제하면_예외() {
        Stock stock = Stock.register(1L, 10);
        stock.reserve(2);

        assertThatThrownBy(() -> stock.releaseReservation(3))
            .isInstanceOf(StockReservationQuantityMismatchException.class);
    }
}
