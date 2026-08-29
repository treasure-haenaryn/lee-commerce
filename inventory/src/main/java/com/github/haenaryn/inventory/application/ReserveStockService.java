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
        // 동일 (orderId, stockId) 재요청이 위 findByOrderIdAndStockId 이후 동시에
        // 커밋을 시도하면, 늦게 도착한 쪽은 DB UNIQUE(order_id, stock_id) 위반으로
        // DataIntegrityViolationException을 받는다. 여기서 잡아 흡수하지 않고 그대로
        // 전파한다 — 호출자가 reserve() 전체를 재시도하면 그때는 이미 커밋된 예약을
        // findByOrderIdAndStockId가 정상적으로 찾아 반환한다.
        StockReservation saved = stockReservationRepository.save(reservation);

        return new ReserveStockResult(saved.getId(), stock.getId(), saved.getStatus());
    }
}
