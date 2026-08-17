package com.github.haenaryn.catalog.infrastructure.search;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogOutboxEventGatewayTest {

    @Mock
    private CatalogOutboxEventJpaRepository outboxEventJpaRepository;

    private CatalogOutboxEventGateway gateway;

    @BeforeEach
    void setUp() {
        gateway = new CatalogOutboxEventGateway(outboxEventJpaRepository);
    }

    @Test
    void claimBatch은_조회한_이벤트를_PROCESSING으로_바꾼다() {
        CatalogOutboxEvent event = new CatalogOutboxEvent("Product", 1L, "ProductChanged", "{}");
        when(outboxEventJpaRepository.findClaimableBatchForUpdateSkipLocked(any(), anyInt())).thenReturn(List.of(event));

        List<CatalogOutboxEvent> claimed = gateway.claimBatch(10);

        assertThat(claimed).containsExactly(event);
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PROCESSING);
    }

    @Test
    void markPublished은_이벤트를_PUBLISHED로_바꾸고_저장한다() {
        CatalogOutboxEvent event = new CatalogOutboxEvent("Product", 1L, "ProductChanged", "{}");

        gateway.markPublished(event, Instant.now());

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        verify(outboxEventJpaRepository).saveAndFlush(event);
    }

    @Test
    void markPublished은_리스가_만료돼_저장이_실패하면_예외를_그대로_전파한다() {
        // JPA 스펙상 flush 중 OptimisticLockException이 나면 트랜잭션이 rollback-only로
        // 표시된다 — 여기서 삼키면 @Transactional 프록시가 커밋 시점에
        // UnexpectedRollbackException을 던지므로, 애초에 삼키지 않고 호출자(트랜잭션
        // 밖)로 전파시켜야 한다.
        CatalogOutboxEvent event = new CatalogOutboxEvent("Product", 1L, "ProductChanged", "{}");
        when(outboxEventJpaRepository.saveAndFlush(event)).thenThrow(new OptimisticLockingFailureException("stale"));

        assertThatThrownBy(() -> gateway.markPublished(event, Instant.now()))
            .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void markFailed은_재시도_횟수를_늘리고_저장한다() {
        CatalogOutboxEvent event = new CatalogOutboxEvent("Product", 1L, "ProductChanged", "{}");

        gateway.markFailed(event);

        assertThat(event.getRetryCount()).isEqualTo(1);
        verify(outboxEventJpaRepository).saveAndFlush(event);
    }
}
