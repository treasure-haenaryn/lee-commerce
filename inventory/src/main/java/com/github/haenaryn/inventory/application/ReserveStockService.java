package com.github.haenaryn.inventory.application;

import com.github.haenaryn.inventory.domain.Stock;
import com.github.haenaryn.inventory.domain.StockReservation;
import com.github.haenaryn.inventory.domain.StockReservationRepository;
import com.github.haenaryn.inventory.domain.StockRepository;
import com.github.haenaryn.inventory.domain.exception.StockNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReserveStockService {

    private final StockRepository stockRepository;
    private final StockReservationRepository stockReservationRepository;

    @Transactional
    public ReserveStockResult reserve(ReserveStockCommand command) {
        Stock stock = stockRepository.findByProductOptionId(command.productOptionId())
            .orElseThrow(() -> new StockNotFoundException(command.productOptionId()));

        Optional<StockReservation> existing =
            stockReservationRepository.findByOrderIdAndStockId(command.orderId(), stock.getId());
        if (existing.isPresent()) {
            StockReservation reservation = existing.get();
            return new ReserveStockResult(reservation.getId(), stock.getId(), reservation.getStatus());
        }

        stock.reserve(command.quantity());
        StockReservation reservation =
            StockReservation.create(stock.getId(), command.orderId(), command.quantity());

        stockRepository.save(stock);
        StockReservation saved = stockReservationRepository.save(reservation);

        return new ReserveStockResult(saved.getId(), stock.getId(), saved.getStatus());
    }
}
