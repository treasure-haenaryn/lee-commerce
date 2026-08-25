package com.github.haenaryn.inventory.application;

import com.github.haenaryn.inventory.domain.Stock;
import com.github.haenaryn.inventory.domain.StockReservation;
import com.github.haenaryn.inventory.domain.StockReservationRepository;
import com.github.haenaryn.inventory.domain.StockReservationStatus;
import com.github.haenaryn.inventory.domain.StockRepository;
import com.github.haenaryn.inventory.domain.exception.InvalidStockReservationStateException;
import com.github.haenaryn.inventory.domain.exception.StockNotFoundException;
import com.github.haenaryn.inventory.domain.exception.StockReservationNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmStockServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private StockReservationRepository stockReservationRepository;

    private ConfirmStockService service;

    @BeforeEach
    void setUp() {
        service = new ConfirmStockService(stockRepository, stockReservationRepository);
    }

    private Stock stockWithId(int availableQuantity) {
        Stock stock = Stock.register(1L, availableQuantity);
        ReflectionTestUtils.setField(stock, "id", 10L);
        return stock;
    }

    @Test
    void 재고가_없으면_예외() {
        when(stockRepository.findByProductOptionId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirm(new ConfirmStockCommand(1L, 100L)))
            .isInstanceOf(StockNotFoundException.class);
    }

    @Test
    void 예약_이력이_없으면_예외() {
        Stock stock = stockWithId(10);
        when(stockRepository.findByProductOptionId(1L)).thenReturn(Optional.of(stock));
        when(stockReservationRepository.findByOrderIdAndStockId(100L, stock.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirm(new ConfirmStockCommand(1L, 100L)))
            .isInstanceOf(StockReservationNotFoundException.class);
    }

    @Test
    void 이미_확정된_예약이면_아무_것도_하지_않는다() {
        Stock stock = stockWithId(10);
        stock.reserve(3);
        StockReservation reservation = StockReservation.create(stock.getId(), 100L, 3);
        reservation.confirm();
        when(stockRepository.findByProductOptionId(1L)).thenReturn(Optional.of(stock));
        when(stockReservationRepository.findByOrderIdAndStockId(100L, stock.getId()))
            .thenReturn(Optional.of(reservation));

        service.confirm(new ConfirmStockCommand(1L, 100L));

        verify(stockRepository, never()).save(any());
        verify(stockReservationRepository, never()).save(any());
    }

    @Test
    void 확정하면_예약_수량을_줄이고_저장한다() {
        Stock stock = stockWithId(10);
        stock.reserve(3);
        StockReservation reservation = StockReservation.create(stock.getId(), 100L, 3);
        when(stockRepository.findByProductOptionId(1L)).thenReturn(Optional.of(stock));
        when(stockReservationRepository.findByOrderIdAndStockId(100L, stock.getId()))
            .thenReturn(Optional.of(reservation));

        service.confirm(new ConfirmStockCommand(1L, 100L));

        assertThat(stock.getAvailableQuantity()).isEqualTo(7);
        assertThat(stock.getReservedQuantity()).isEqualTo(0);
        assertThat(reservation.getStatus()).isEqualTo(StockReservationStatus.CONFIRMED);
        verify(stockRepository).save(stock);
        verify(stockReservationRepository).save(reservation);
    }

    @Test
    void 이미_해제된_예약을_확정하려_하면_예외() {
        Stock stock = stockWithId(10);
        stock.reserve(3);
        StockReservation reservation = StockReservation.create(stock.getId(), 100L, 3);
        reservation.release();
        when(stockRepository.findByProductOptionId(1L)).thenReturn(Optional.of(stock));
        when(stockReservationRepository.findByOrderIdAndStockId(100L, stock.getId()))
            .thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> service.confirm(new ConfirmStockCommand(1L, 100L)))
            .isInstanceOf(InvalidStockReservationStateException.class);
    }
}
