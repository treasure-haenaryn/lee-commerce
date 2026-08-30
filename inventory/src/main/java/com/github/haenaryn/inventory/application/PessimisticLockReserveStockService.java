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
public class PessimisticLockReserveStockService {

    private final StockRepository stockRepository;
    private final StockReservationRepository stockReservationRepository;

    @Transactional
    public ReserveStockResult reserve(ReserveStockCommand command) {
        Stock stock = stockRepository.findByProductOptionIdForUpdate(command.productOptionId())
            .orElseThrow(() -> new StockNotFoundException(command.productOptionId()));

        // Stock 행을 이미 잠근 뒤에 멱등성 조회를 하므로, 동일 (orderId, stockId)로
        // 동시에 들어온 재시도는 여기서 직렬화된다 — 먼저 커밋된 예약이 있으면 뒤에
        // 온 요청은 그 예약을 그대로 찾아 반환하고, 유일성 제약 위반이나 재시도
        // 계약(ReserveStockService 참고)이 애초에 필요 없다.
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
