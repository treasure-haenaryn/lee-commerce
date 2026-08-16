package com.github.haenaryn.catalog.application;

import com.github.haenaryn.catalog.domain.Product;
import com.github.haenaryn.catalog.domain.ProductRepository;
import com.github.haenaryn.catalog.domain.exception.ProductNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeactivateProductOptionService {

    private final ProductRepository productRepository;

    @Transactional
    public void deactivate(DeactivateProductOptionCommand command) {
        Product product = productRepository.findById(command.productId())
            .orElseThrow(() -> new ProductNotFoundException(command.productId()));

        product.deactivateOption(command.skuCode());

        productRepository.save(product);
    }
}
