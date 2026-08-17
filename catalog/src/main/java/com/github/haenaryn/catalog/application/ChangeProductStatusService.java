package com.github.haenaryn.catalog.application;

import com.github.haenaryn.catalog.domain.Product;
import com.github.haenaryn.catalog.domain.ProductRepository;
import com.github.haenaryn.catalog.domain.ProductStatus;
import com.github.haenaryn.catalog.domain.event.ProductChangedEvent;
import com.github.haenaryn.catalog.domain.exception.ProductNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChangeProductStatusService {

    private final ProductRepository productRepository;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional
    public void changeStatus(ChangeProductStatusCommand command) {
        Product product = productRepository.findById(command.productId())
            .orElseThrow(() -> new ProductNotFoundException(command.productId()));

        product.changeStatus(ProductStatus.valueOf(command.status()));

        productRepository.save(product);
        domainEventPublisher.publish(new ProductChangedEvent(product.getId()));
    }
}
