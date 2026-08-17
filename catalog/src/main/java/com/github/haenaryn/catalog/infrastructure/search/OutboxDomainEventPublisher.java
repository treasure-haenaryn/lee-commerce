package com.github.haenaryn.catalog.infrastructure.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.haenaryn.catalog.application.DomainEventPublisher;
import com.github.haenaryn.catalog.domain.event.ProductChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class OutboxDomainEventPublisher implements DomainEventPublisher {

    private static final String AGGREGATE_TYPE_PRODUCT = "Product";
    private static final String EVENT_TYPE_PRODUCT_CHANGED = "ProductChanged";

    private final CatalogOutboxEventJpaRepository outboxEventJpaRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(ProductChangedEvent event) {
        outboxEventJpaRepository.save(new CatalogOutboxEvent(
            AGGREGATE_TYPE_PRODUCT, event.productId(), EVENT_TYPE_PRODUCT_CHANGED, writePayload(event)));
    }

    private String writePayload(ProductChangedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("ProductChangedEvent 직렬화에 실패했다: " + event, e);
        }
    }
}
