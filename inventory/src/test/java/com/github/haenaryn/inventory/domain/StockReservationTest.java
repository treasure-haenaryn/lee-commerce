package com.github.haenaryn.inventory.domain;

import com.github.haenaryn.inventory.domain.exception.InvalidStockReservationStateException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockReservationTest {

    @Test
    void 재고_ID가_없으면_생성에_실패한다() {
        assertThatThrownBy(() -> StockReservation.create(null, 1L, 1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 주문_ID가_없으면_생성에_실패한다() {
        assertThatThrownBy(() -> StockReservation.create(1L, null, 1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 수량이_0이하면_생성에_실패한다() {
        assertThatThrownBy(() -> StockReservation.create(1L, 1L, 0))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 생성하면_상태는_RESERVED다() {
        StockReservation reservation = StockReservation.create(1L, 1L, 3);

        assertThat(reservation.getStatus()).isEqualTo(StockReservationStatus.RESERVED);
    }

    @Test
    void 예약_상태에서_확정하면_CONFIRMED가_된다() {
        StockReservation reservation = StockReservation.create(1L, 1L, 3);

        reservation.confirm();

        assertThat(reservation.getStatus()).isEqualTo(StockReservationStatus.CONFIRMED);
    }

    @Test
    void 예약_상태에서_해제하면_RELEASED가_된다() {
        StockReservation reservation = StockReservation.create(1L, 1L, 3);

        reservation.release();

        assertThat(reservation.getStatus()).isEqualTo(StockReservationStatus.RELEASED);
    }

    @Test
    void 이미_확정된_예약을_다시_확정하면_예외() {
        StockReservation reservation = StockReservation.create(1L, 1L, 3);
        reservation.confirm();

        assertThatThrownBy(reservation::confirm)
            .isInstanceOf(InvalidStockReservationStateException.class);
    }

    @Test
    void 이미_해제된_예약을_확정하면_예외() {
        StockReservation reservation = StockReservation.create(1L, 1L, 3);
        reservation.release();

        assertThatThrownBy(reservation::confirm)
            .isInstanceOf(InvalidStockReservationStateException.class);
    }

    @Test
    void 이미_확정된_예약을_해제하면_예외() {
        StockReservation reservation = StockReservation.create(1L, 1L, 3);
        reservation.confirm();

        assertThatThrownBy(reservation::release)
            .isInstanceOf(InvalidStockReservationStateException.class);
    }
}
