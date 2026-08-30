package com.github.haenaryn.inventory.application;

import com.github.haenaryn.inventory.domain.Stock;
import com.github.haenaryn.inventory.domain.StockReservation;
import com.github.haenaryn.inventory.domain.StockReservationRepository;
import com.github.haenaryn.inventory.domain.StockReservationStatus;
import com.github.haenaryn.inventory.domain.StockRepository;
import com.github.haenaryn.inventory.domain.exception.InsufficientStockException;
import com.github.haenaryn.inventory.domain.exception.StockNotFoundException;
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
class PessimisticLockReserveStockServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private StockReservationRepository stockReservationRepository;

    private PessimisticLockReserveStockService service;

    @BeforeEach
    void setUp() {
        service = new PessimisticLockReserveStockService(stockRepository, stockReservationRepository);
    }

    @Test
    void 재고가_없으면_예외() {
        when(stockRepository.findByProductOptionIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reserve(new ReserveStockCommand(1L, 100L, 3)))
            .isInstanceOf(StockNotFoundException.class);
    }

    @Test
    void 가용_수량이_부족하면_예외() {
        Stock stock = Stock.register(1L, 2);
        ReflectionTestUtils.setField(stock, "id", 10L);
        when(stockRepository.findByProductOptionIdForUpdate(1L)).thenReturn(Optional.of(stock));
        when(stockReservationRepository.findByOrderIdAndStockId(100L, stock.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reserve(new ReserveStockCommand(1L, 100L, 3)))
            .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void 이미_예약이_존재하면_재고를_다시_차감하지_않고_기존_예약을_반환한다() {
        Stock stock = Stock.register(1L, 10);
        ReflectionTestUtils.setField(stock, "id", 10L);
        StockReservation existing = StockReservation.create(stock.getId(), 100L, 3);
        when(stockRepository.findByProductOptionIdForUpdate(1L)).thenReturn(Optional.of(stock));
        when(stockReservationRepository.findByOrderIdAndStockId(100L, stock.getId()))
            .thenReturn(Optional.of(existing));

        ReserveStockResult result = service.reserve(new ReserveStockCommand(1L, 100L, 3));

        assertThat(result.status()).isEqualTo(StockReservationStatus.RESERVED);
        assertThat(stock.getAvailableQuantity()).isEqualTo(10);
        verify(stockRepository, never()).save(any());
        verify(stockReservationRepository, never()).save(any());
    }

    @Test
    void 예약하면_재고를_차감하고_예약을_저장한다() {
        Stock stock = Stock.register(1L, 10);
        ReflectionTestUtils.setField(stock, "id", 10L);
        StockReservation saved = StockReservation.create(stock.getId(), 100L, 3);
        when(stockRepository.findByProductOptionIdForUpdate(1L)).thenReturn(Optional.of(stock));
        when(stockReservationRepository.findByOrderIdAndStockId(100L, stock.getId())).thenReturn(Optional.empty());
        when(stockReservationRepository.save(any(StockReservation.class))).thenReturn(saved);

        ReserveStockResult result = service.reserve(new ReserveStockCommand(1L, 100L, 3));

        assertThat(stock.getAvailableQuantity()).isEqualTo(7);
        assertThat(stock.getReservedQuantity()).isEqualTo(3);
        assertThat(result.status()).isEqualTo(StockReservationStatus.RESERVED);
        verify(stockRepository).save(stock);
        verify(stockReservationRepository).save(any(StockReservation.class));
    }
}
