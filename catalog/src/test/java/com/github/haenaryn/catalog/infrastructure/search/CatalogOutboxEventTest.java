package com.github.haenaryn.catalog.infrastructure.search;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogOutboxEventTest {

    private CatalogOutboxEvent event() {
        return new CatalogOutboxEvent("Product", 1L, "ProductChanged", "{\"productId\":1}");
    }

    @Test
    void 생성_직후_상태는_PENDING이다() {
        CatalogOutboxEvent event = event();

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(event.getRetryCount()).isZero();
    }

    @Test
    void 폴러가_집어가면_PROCESSING으로_바뀐다() {
        CatalogOutboxEvent event = event();

        event.markProcessing(Instant.now());

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PROCESSING);
    }

    @Test
    void 발행에_성공하면_PUBLISHED로_바뀐다() {
        CatalogOutboxEvent event = event();
        Instant now = Instant.now();

        event.markPublished(now);

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
    }

    @Test
    void 재시도_횟수가_최대치_미만이면_실패해도_PENDING을_유지한다() {
        CatalogOutboxEvent event = event();

        for (int i = 0; i < 4; i++) {
            event.markFailed();
        }

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(event.getRetryCount()).isEqualTo(4);
    }

    @Test
    void 재시도_횟수가_최대치에_도달하면_FAILED로_고정된다() {
        CatalogOutboxEvent event = event();

        for (int i = 0; i < 5; i++) {
            event.markFailed();
        }

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(event.getRetryCount()).isEqualTo(5);
    }
}
