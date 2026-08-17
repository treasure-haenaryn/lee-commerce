package com.github.haenaryn.catalog.infrastructure.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.haenaryn.catalog.domain.event.ProductChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxDomainEventPublisherTest {

    @Mock
    private CatalogOutboxEventJpaRepository outboxEventJpaRepository;

    private OutboxDomainEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OutboxDomainEventPublisher(outboxEventJpaRepository, new ObjectMapper());
    }

    @Test
    void 상품_변경_이벤트를_outbox에_저장한다() {
        publisher.publish(new ProductChangedEvent(42L));

        ArgumentCaptor<CatalogOutboxEvent> captor = ArgumentCaptor.forClass(CatalogOutboxEvent.class);
        verify(outboxEventJpaRepository).save(captor.capture());

        CatalogOutboxEvent saved = captor.getValue();
        assertThat(saved.getAggregateType()).isEqualTo("Product");
        assertThat(saved.getAggregateId()).isEqualTo(42L);
        assertThat(saved.getEventType()).isEqualTo("ProductChanged");
        assertThat(saved.getPayload()).contains("42");
        assertThat(saved.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
    }
}
