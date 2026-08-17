package com.github.haenaryn.catalog.application;

import com.github.haenaryn.catalog.domain.event.ProductChangedEvent;

public interface DomainEventPublisher {

    void publish(ProductChangedEvent event);
}
