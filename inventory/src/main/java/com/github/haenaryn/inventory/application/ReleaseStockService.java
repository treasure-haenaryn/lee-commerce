package com.github.haenaryn.inventory.application;

import com.github.haenaryn.inventory.domain.Stock;
import com.github.haenaryn.inventory.domain.StockReservation;
import com.github.haenaryn.inventory.domain.StockReservationRepository;
import com.github.haenaryn.inventory.domain.StockReservationStatus;
import com.github.haenaryn.inventory.domain.StockRepository;
import com.github.haenaryn.inventory.domain.exception.StockNotFoundException;
import com.github.haenaryn.inventory.domain.exception.StockReservationNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReleaseStockService {

    private final StockRepository stockRepository;
    private final StockReservationRepository stockReservationRepository;

    @Transactional
    public void release(ReleaseStockCommand command) {
        Stock stock = stockRepository.findByProductOptionId(command.productOptionId())
            .orElseThrow(() -> new StockNotFoundException(command.productOptionId()));

        StockReservation reservation = stockReservationRepository
            .findByOrderIdAndStockId(command.orderId(), stock.getId())
            .orElseThrow(() -> new StockReservationNotFoundException(command.orderId(), stock.getId()));

        if (reservation.getStatus() == StockReservationStatus.RELEASED) {
            return;
        }

        stock.releaseReservation(reservation.getQuantity());
        reservation.release();

        stockRepository.save(stock);
        stockReservationRepository.save(reservation);
    }
}
