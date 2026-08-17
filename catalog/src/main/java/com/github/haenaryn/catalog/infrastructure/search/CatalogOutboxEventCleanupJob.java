package com.github.haenaryn.catalog.infrastructure.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

// PUBLISHED 행을 계속 쌓아두면 outbox_events 테이블과 인덱스가 무한정 커진다. FAILED
// 행은 원인 조사가 끝날 때까지 필요할 수 있어 더 오래 보존한다.
@Slf4j
@Component
@RequiredArgsConstructor
class CatalogOutboxEventCleanupJob {

    private static final int PUBLISHED_RETENTION_DAYS = 7;
    private static final int DELETE_BATCH_SIZE = 500;

    private final CatalogOutboxEventJpaRepository outboxEventJpaRepository;

    // 삭제 대상이 많이 쌓여 있어도 트랜잭션 하나가 길게 잠금을 쥐지 않도록, 이 메서드
    // 자체는 트랜잭션을 열지 않고 Repository의 배치 삭제 호출을 반복한다 — 각 호출은
    // Spring Data가 개별적으로 짧은 트랜잭션을 열어준다.
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupPublishedEvents() {
        Instant threshold = Instant.now().minus(PUBLISHED_RETENTION_DAYS, ChronoUnit.DAYS);
        long totalDeleted = 0;
        int deletedInBatch;
        do {
            deletedInBatch = outboxEventJpaRepository.deletePublishedBatchBefore(threshold, DELETE_BATCH_SIZE);
            totalDeleted += deletedInBatch;
        } while (deletedInBatch == DELETE_BATCH_SIZE);

        if (totalDeleted > 0) {
            log.info("보존 기간이 지난 outbox 이벤트를 정리했다. count={}", totalDeleted);
        }
    }
}
