package com.github.haenaryn.catalog.infrastructure.search;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

interface CatalogOutboxEventJpaRepository extends JpaRepository<CatalogOutboxEvent, Long> {

    // order.outbox_events 폴러와 동일한 방식: SKIP LOCKED로 다른 인스턴스가 이미 집어간
    // 행은 건너뛴다 — 여러 인스턴스가 동시에 떠 있어도 같은 이벤트를 중복 처리하지 않는다.
    // PENDING뿐 아니라 리스가 만료된 PROCESSING 행도 함께 골라 재선점한다 — 그렇지
    // 않으면 폴러가 색인 도중 죽었을 때 그 행이 PROCESSING에 영원히 멈춰 있게 된다.
    @Query(
        value = "SELECT * FROM catalog.outbox_events "
            + "WHERE status = 'PENDING' OR (status = 'PROCESSING' AND processing_at < :staleThreshold) "
            + "ORDER BY created_at LIMIT :batchSize FOR UPDATE SKIP LOCKED",
        nativeQuery = true)
    List<CatalogOutboxEvent> findClaimableBatchForUpdateSkipLocked(Instant staleThreshold, int batchSize);

    // 한 번에 지울 행 수를 제한한다 — cleanup 대상이 많이 쌓여 있어도 트랜잭션 하나가
    // 길게 잠금을 쥐지 않도록 호출부(CatalogOutboxEventCleanupJob)에서 이 메서드를
    // 반복 호출한다. @Modifying 쿼리는 트랜잭션이 필요해서, 호출부(cleanupPublishedEvents)가
    // 아니라 이 메서드 자체에 짧은 트랜잭션을 건다 — 호출부까지 트랜잭션을 걸면 반복 전체가
    // 하나의 긴 트랜잭션으로 묶여 배치로 나눈 의미가 없어진다.
    @Transactional
    @Modifying
    @Query(
        value = "DELETE FROM catalog.outbox_events WHERE id IN ("
            + "SELECT id FROM catalog.outbox_events "
            + "WHERE status = 'PUBLISHED' AND published_at < :threshold LIMIT :batchSize)",
        nativeQuery = true)
    int deletePublishedBatchBefore(Instant threshold, int batchSize);
}
