package com.github.haenaryn.inventory.infrastructure.persistence;

import com.github.haenaryn.inventory.application.ReserveStockCommand;
import com.github.haenaryn.inventory.application.ReserveStockService;
import com.github.haenaryn.inventory.domain.Stock;
import com.github.haenaryn.inventory.domain.StockRepository;
import com.github.haenaryn.inventory.domain.StockReservationRepository;
import com.github.haenaryn.inventory.domain.exception.InsufficientStockException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

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

class StockConcurrentReservationIntegrationTest extends AbstractInventoryIntegrationTest {

    private static final int INITIAL_QUANTITY = 10;
    private static final int CONCURRENT_ORDERS = 20;
    // 이 테스트는 각 스레드가 ReserveStockService.reserve()를 직접 커밋하므로 트랜잭션
    // 하나로 묶여 롤백되지 않는다. 다른 통합 테스트 클래스와 같은 컨테이너 DB를 공유하므로
    // product_option_id 유일성 제약과 충돌하지 않도록 전용 값을 쓴다.
    private static final Long PRODUCT_OPTION_ID = 9999L;
    private static final long ORDER_ID_BASE = 900_000L;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockReservationRepository stockReservationRepository;

    @Autowired
    private ReserveStockService reserveStockService;

    @Test
    void 서로_다른_주문이_동시에_예약해도_오버셀_없이_예약_이력이_정확히_남는다() throws InterruptedException {
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
        assertThat(successCount).isBetween(1L, (long) INITIAL_QUANTITY);
        assertThat(finalStock.getAvailableQuantity()).isEqualTo(INITIAL_QUANTITY - (int) successCount);
        assertThat(finalStock.getReservedQuantity()).isEqualTo((int) successCount);
        assertThat(finalStock.getAvailableQuantity()).isGreaterThanOrEqualTo(0);

        // 성공/실패한 주문마다 예약 이력이 실제로 정확히 남았는지(누락도 중복도 없이)
        // ReserveStockService 전체 유스케이스를 통해 검증한다 — Stock 수량만 보는 것으로는
        // StockReservation 생성이 빠지거나 중복되는 문제를 잡지 못한다.
        outcomes.forEach((orderId, succeeded) -> {
            boolean reservationExists =
                stockReservationRepository.findByOrderIdAndStockId(orderId, stockId).isPresent();
            assertThat(reservationExists).as("주문 %d의 예약 이력 존재 여부", orderId).isEqualTo(succeeded);
        });
    }

    private boolean attemptReserve(Long orderId) {
        try {
            reserveStockService.reserve(new ReserveStockCommand(PRODUCT_OPTION_ID, orderId, 1));
            return true;
        } catch (ObjectOptimisticLockingFailureException | InsufficientStockException expected) {
            // 둘 다 오버셀 대신 정직한 실패다 — 어느 쪽이 발생하는지는 이 스레드가 재고를
            // 읽은 시점에 이미 다른 커밋으로 소진됐는지(InsufficientStockException, 읽기
            // 시점에 이미 0), 아니면 읽은 뒤 다른 트랜잭션이 먼저 커밋해버렸는지
            // (ObjectOptimisticLockingFailureException, 쓰기 시점 버전 충돌)에 달려 있고
            // 스레드 스케줄링에 좌우되므로 테스트가 어느 쪽을 강제할 수 없다.
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
