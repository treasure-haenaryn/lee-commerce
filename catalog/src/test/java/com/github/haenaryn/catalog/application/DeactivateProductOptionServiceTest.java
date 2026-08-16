package com.github.haenaryn.catalog.application;

import com.github.haenaryn.catalog.domain.Product;
import com.github.haenaryn.catalog.domain.ProductOptionSpec;
import com.github.haenaryn.catalog.domain.ProductRepository;
import com.github.haenaryn.catalog.domain.exception.ProductNotFoundException;
import com.github.haenaryn.common.vo.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeactivateProductOptionServiceTest {

    @Mock
    private ProductRepository productRepository;

    private DeactivateProductOptionService service;

    @BeforeEach
    void setUp() {
        service = new DeactivateProductOptionService(productRepository);
    }

    @Test
    void 상품이_없으면_예외() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivate(new DeactivateProductOptionCommand(1L, "SKU-1")))
            .isInstanceOf(ProductNotFoundException.class);

        verify(productRepository, never()).save(any());
    }

    @Test
    void 옵션을_비활성화하면_저장한다() {
        Product product = Product.register(1L, "티셔츠", null,
            new Money(new BigDecimal("10000"), Currency.getInstance("KRW")),
            List.of(new ProductOptionSpec("SKU-1", null, null, null), new ProductOptionSpec("SKU-2", null, null, null)));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        service.deactivate(new DeactivateProductOptionCommand(1L, "SKU-1"));

        assertThat(product.getOptions())
            .filteredOn(option -> option.getSkuCode().equals("SKU-1"))
            .allMatch(option -> !option.isActive());
        verify(productRepository).save(product);
    }
}
