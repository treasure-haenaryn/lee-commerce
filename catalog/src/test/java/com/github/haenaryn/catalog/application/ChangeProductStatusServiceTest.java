package com.github.haenaryn.catalog.application;

import com.github.haenaryn.catalog.domain.Product;
import com.github.haenaryn.catalog.domain.ProductOptionSpec;
import com.github.haenaryn.catalog.domain.ProductRepository;
import com.github.haenaryn.catalog.domain.ProductStatus;
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
class ChangeProductStatusServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    private ChangeProductStatusService service;

    @BeforeEach
    void setUp() {
        service = new ChangeProductStatusService(productRepository, domainEventPublisher);
    }

    @Test
    void 상품이_없으면_예외() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeStatus(new ChangeProductStatusCommand(1L, "SOLD_OUT")))
            .isInstanceOf(ProductNotFoundException.class);

        verify(productRepository, never()).save(any());
    }

    @Test
    void 상태를_변경하면_저장한다() {
        Product product = Product.register(1L, "티셔츠", null,
            new Money(new BigDecimal("10000"), Currency.getInstance("KRW")),
            List.of(new ProductOptionSpec("SKU-1", null, null, null)));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        service.changeStatus(new ChangeProductStatusCommand(1L, "SOLD_OUT"));

        assertThat(product.getStatus()).isEqualTo(ProductStatus.SOLD_OUT);
        verify(productRepository).save(product);
        verify(domainEventPublisher).publish(any());
    }
}
