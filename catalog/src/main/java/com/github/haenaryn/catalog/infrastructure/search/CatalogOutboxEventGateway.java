package com.github.haenaryn.catalog.infrastructure.search;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

// 폴러가 DB 트랜잭션을 잡은 채로 Elasticsearch 네트워크 호출을 하지 않도록, 상태
// 전이마다 별도의 짧은 트랜잭션으로 쪼갠다 — claim 한 번, 이벤트별 결과 갱신 한 번씩.
// 색인 자체(외부 호출)는 이 클래스 밖, 트랜잭션 없이 실행된다.
//
// markPublished/markFailed는 낙관적 락 충돌(OptimisticLockingFailureException)을
// 여기서 잡지 않는다 — JPA 스펙상 flush 중 발생한 OptimisticLockException은 그
// 트랜잭션을 rollback-only로 표시하므로, 메서드 안에서 예외를 삼켜 정상 반환해도
// @Transactional 프록시가 커밋을 시도하며 UnexpectedRollbackException을 던진다.
// 그 예외는 이 메서드 try/catch 밖(프록시의 커밋 시점)에서 발생해 여기서는 잡을 수
// 없다. 그래서 예외를 그대로 호출자(ProductSearchIndexOutboxPoller, 트랜잭션 밖)로
// 전파시키고 거기서 처리한다.
@Component
@RequiredArgsConstructor
class CatalogOutboxEventGateway {

    // 폴러가 이벤트를 집어간 뒤 이 시간 안에 처리를 끝내지 못하면(프로세스 중단 포함)
    // 다른 폴러 사이클이 다시 집어갈 수 있는 행으로 취급한다.
    private static final Duration PROCESSING_LEASE_TIMEOUT = Duration.ofMinutes(5);

    private final CatalogOutboxEventJpaRepository outboxEventJpaRepository;

    @Transactional
    List<CatalogOutboxEvent> claimBatch(int batchSize) {
        Instant now = Instant.now();
        Instant staleThreshold = now.minus(PROCESSING_LEASE_TIMEOUT);
        List<CatalogOutboxEvent> claimed =
            outboxEventJpaRepository.findClaimableBatchForUpdateSkipLocked(staleThreshold, batchSize);
        claimed.forEach(event -> event.markProcessing(now));
        return claimed;
    }

    @Transactional
    void markPublished(CatalogOutboxEvent event, Instant publishedAt) {
        event.markPublished(publishedAt);
        outboxEventJpaRepository.saveAndFlush(event);
    }

    @Transactional
    void markFailed(CatalogOutboxEvent event) {
        event.markFailed();
        outboxEventJpaRepository.saveAndFlush(event);
    }
}
