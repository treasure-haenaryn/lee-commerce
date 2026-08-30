package com.github.haenaryn.inventory.infrastructure.persistence;

import com.github.haenaryn.inventory.application.PessimisticLockReserveStockService;
import com.github.haenaryn.inventory.application.ReserveStockCommand;
import com.github.haenaryn.inventory.domain.Stock;
import com.github.haenaryn.inventory.domain.StockRepository;
import com.github.haenaryn.inventory.domain.StockReservationRepository;
import com.github.haenaryn.inventory.domain.exception.InsufficientStockException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class PessimisticLockConcurrentReservationIntegrationTest extends AbstractInventoryIntegrationTest {

    private static final int INITIAL_QUANTITY = 10;
    private static final int CONCURRENT_ORDERS = 20;
    // 다른 통합 테스트 클래스와 같은 컨테이너 DB를 공유하고 이 테스트도 실제로 커밋되므로
    // product_option_id 유일성 제약과 충돌하지 않도록 전용 값을 쓴다.
    private static final Long PRODUCT_OPTION_ID = 9998L;
    private static final long ORDER_ID_BASE = 800_000L;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockReservationRepository stockReservationRepository;

    @Autowired
    private PessimisticLockReserveStockService pessimisticLockReserveStockService;

    @Test
    void 비관적_락은_경합을_직렬화해_가용_수량만큼_결정적으로_전부_성공한다() throws InterruptedException {
        Long stockId = stockRepository.save(Stock.register(PRODUCT_OPTION_ID, INITIAL_QUANTITY)).getId();

        List<Long> orderIds = IntStream.range(0, CONCURRENT_ORDERS)
            .mapToObj(i -> ORDER_ID_BASE + i)
            .collect(Collectors.toList());

        List<Callable<Boolean>> attempts = orderIds.stream()
            .<Callable<Boolean>>map(orderId -> () -> attemptReserve(orderId))
            .collect(Collectors.toList());

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_ORDERS);
        List<Future<Boolean>> futures = executor.invokeAll(attempts, 30, TimeUnit.SECONDS);
        executor.shutdown();

        Map<Long, Boolean> outcomes = new ConcurrentHashMap<>();
        for (int i = 0; i < orderIds.size(); i++) {
            outcomes.put(orderIds.get(i), resolve(futures.get(i)));
        }
        long successCount = outcomes.values().stream().filter(Boolean::booleanValue).count();

        Stock finalStock = stockRepository.findById(stockId).orElseThrow();
        // 낙관적 락(StockConcurrentReservationIntegrationTest)은 경합에서 진 요청이
        // 기술 예외로 실패해 성공 건수가 "1~가용 수량 사이"로만 보장됐다. 비관적 락은
        // 요청을 직렬화하므로 실패한 시도 없이 가용 수량만큼 정확히 성공한다 — 처리량은
        // 낮아지는 대신 결정적이다.
        assertThat(successCount).isEqualTo(INITIAL_QUANTITY);
        assertThat(finalStock.getAvailableQuantity()).isZero();
        assertThat(finalStock.getReservedQuantity()).isEqualTo(INITIAL_QUANTITY);

        outcomes.forEach((orderId, succeeded) -> {
            boolean reservationExists =
                stockReservationRepository.findByOrderIdAndStockId(orderId, stockId).isPresent();
            assertThat(reservationExists).as("주문 %d의 예약 이력 존재 여부", orderId).isEqualTo(succeeded);
        });
    }

    private boolean attemptReserve(Long orderId) {
        try {
            pessimisticLockReserveStockService.reserve(new ReserveStockCommand(PRODUCT_OPTION_ID, orderId, 1));
            return true;
        } catch (InsufficientStockException expected) {
            // 비관적 락은 행을 잠그고 순서대로 처리하므로 늦게 잠금을 얻은 요청은 이미
            // 소진된 가용 수량을 그대로 보고 정직하게 실패한다 — 기술 예외(낙관적 락
            // 충돌)는 이 전략에서 구조적으로 발생하지 않는다.
            return false;
        }
    }

    private boolean resolve(Future<Boolean> future) {
        try {
            return future.get();
        } catch (ExecutionException | InterruptedException e) {
            throw new AssertionError("예약 스레드가 예상하지 못한 예외로 실패했다", e);
        }
    }
}
