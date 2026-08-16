package com.github.haenaryn.catalog.application;

import com.github.haenaryn.catalog.domain.Product;
import com.github.haenaryn.catalog.domain.ProductOptionSpec;
import com.github.haenaryn.catalog.domain.ProductRepository;
import com.github.haenaryn.catalog.domain.exception.ProductNotFoundException;
import com.github.haenaryn.common.vo.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Currency;

@Service
@RequiredArgsConstructor
public class AddProductOptionService {

    private final ProductRepository productRepository;

    @Transactional
    public void addOption(AddProductOptionCommand command) {
        Product product = productRepository.findById(command.productId())
            .orElseThrow(() -> new ProductNotFoundException(command.productId()));

        Money priceOverride = command.priceOverrideAmount() == null
            ? null
            : new Money(command.priceOverrideAmount(), Currency.getInstance(command.priceOverrideCurrency()));
        product.addOption(new ProductOptionSpec(command.skuCode(), command.size(), command.color(), priceOverride));

        productRepository.save(product);
    }
}
