package com.github.haenaryn.inventory.infrastructure.persistence;

import com.github.haenaryn.inventory.application.PessimisticLockReserveStockService;
import com.github.haenaryn.inventory.application.ReserveStockCommand;
import com.github.haenaryn.inventory.domain.Stock;
import com.github.haenaryn.inventory.domain.StockRepository;
import com.github.haenaryn.inventory.domain.StockReservationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class PessimisticLockDuplicateReservationIntegrationTest extends AbstractInventoryIntegrationTest {

    private static final int INITIAL_QUANTITY = 10;
    private static final Long PRODUCT_OPTION_ID = 9997L;
    private static final Long ORDER_ID = 700_000L;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockReservationRepository stockReservationRepository;

    @Autowired
    private PessimisticLockReserveStockService pessimisticLockReserveStockService;

    @Test
    void 같은_주문이_동시에_두번_예약해도_예외_없이_예약이_하나만_생긴다() throws Exception {
        Long stockId = stockRepository.save(Stock.register(PRODUCT_OPTION_ID, INITIAL_QUANTITY)).getId();

        Callable<Long> attempt = () ->
            pessimisticLockReserveStockService.reserve(new ReserveStockCommand(PRODUCT_OPTION_ID, ORDER_ID, 1))
                .reservationId();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        // 비관적 락은 잠금 구간 안에서 멱등성 조회까지 하므로, 나중에 잠금을 얻은 쪽은
        // 유일성 제약 위반 없이 먼저 커밋된 예약을 그대로 찾아 반환한다 — 낙관적 락
        // 경로(ReserveStockService)가 DataIntegrityViolationException 전파 계약을
        // 문서화해야 했던 바로 그 레이스가 여기서는 구조적으로 발생하지 않는다.
        List<Future<Long>> futures = executor.invokeAll(List.of(attempt, attempt), 30, TimeUnit.SECONDS);
        executor.shutdown();

        Long firstReservationId = futures.get(0).get();
        Long secondReservationId = futures.get(1).get();

        assertThat(firstReservationId).isEqualTo(secondReservationId);

        Stock finalStock = stockRepository.findById(stockId).orElseThrow();
        assertThat(finalStock.getAvailableQuantity()).isEqualTo(INITIAL_QUANTITY - 1);
        assertThat(finalStock.getReservedQuantity()).isEqualTo(1);
        assertThat(stockReservationRepository.findByOrderIdAndStockId(ORDER_ID, stockId)).isPresent();
    }
}
