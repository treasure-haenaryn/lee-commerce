package com.github.haenaryn.catalog.infrastructure.search;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogOutboxEventCleanupJobTest {

    @Mock
    private CatalogOutboxEventJpaRepository outboxEventJpaRepository;

    private CatalogOutboxEventCleanupJob job;

    @BeforeEach
    void setUp() {
        job = new CatalogOutboxEventCleanupJob(outboxEventJpaRepository);
    }

    @Test
    void 삭제_대상이_한_배치보다_적으면_한_번만_호출한다() {
        when(outboxEventJpaRepository.deletePublishedBatchBefore(any(), eq(500))).thenReturn(3);

        job.cleanupPublishedEvents();

        verify(outboxEventJpaRepository, times(1)).deletePublishedBatchBefore(any(), eq(500));
    }

    @Test
    void 삭제_대상이_한_배치를_가득_채우면_다음_배치를_이어서_지운다() {
        when(outboxEventJpaRepository.deletePublishedBatchBefore(any(), eq(500)))
            .thenReturn(500)
            .thenReturn(120);

        job.cleanupPublishedEvents();

        verify(outboxEventJpaRepository, times(2)).deletePublishedBatchBefore(any(), eq(500));
    }
}
