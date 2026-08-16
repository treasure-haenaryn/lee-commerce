package com.github.haenaryn.catalog.application;

import com.github.haenaryn.catalog.domain.Product;
import com.github.haenaryn.catalog.domain.ProductOption;
import com.github.haenaryn.catalog.domain.ProductOptionSpec;
import com.github.haenaryn.catalog.domain.ProductRepository;
import com.github.haenaryn.common.vo.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Currency;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegisterProductService {

    private final ProductRepository productRepository;

    @Transactional
    public RegisterProductResult register(RegisterProductCommand command) {
        Money basePrice = new Money(command.basePriceAmount(), Currency.getInstance(command.basePriceCurrency()));
        List<ProductOptionSpec> optionSpecs = command.options().stream()
            .map(this::toSpec)
            .toList();

        Product product = Product.register(
            command.categoryId(), command.name(), command.description(), basePrice, optionSpecs);
        Product saved = productRepository.save(product);

        List<String> skuCodes = saved.getOptions().stream().map(ProductOption::getSkuCode).toList();
        return new RegisterProductResult(saved.getId(), saved.getName(), saved.getStatus().name(), skuCodes);
    }

    private ProductOptionSpec toSpec(ProductOptionSpecCommand command) {
        Money priceOverride = command.priceOverrideAmount() == null
            ? null
            : new Money(command.priceOverrideAmount(), Currency.getInstance(command.priceOverrideCurrency()));
        return new ProductOptionSpec(command.skuCode(), command.size(), command.color(), priceOverride);
    }
}
