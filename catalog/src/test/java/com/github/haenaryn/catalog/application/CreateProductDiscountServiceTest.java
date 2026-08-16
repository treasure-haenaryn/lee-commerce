package com.github.haenaryn.catalog.application;

import com.github.haenaryn.catalog.domain.Product;
import com.github.haenaryn.catalog.domain.ProductDiscount;
import com.github.haenaryn.catalog.domain.ProductDiscountRepository;
import com.github.haenaryn.catalog.domain.ProductOptionSpec;
import com.github.haenaryn.catalog.domain.ProductRepository;
import com.github.haenaryn.catalog.domain.exception.OverlappingDiscountException;
import com.github.haenaryn.catalog.domain.exception.ProductNotFoundException;
import com.github.haenaryn.common.vo.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
class CreateProductDiscountServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductDiscountRepository productDiscountRepository;

    private CreateProductDiscountService service;

    @BeforeEach
    void setUp() {
        service = new CreateProductDiscountService(productRepository, productDiscountRepository);
    }

    private Product product() {
        return Product.register(1L, "티셔츠", null,
            new Money(new BigDecimal("10000"), Currency.getInstance("KRW")),
            List.of(new ProductOptionSpec("SKU-1", null, null, null)));
    }

    @Test
    void 상품이_없으면_예외() {
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(
            new CreateProductDiscountCommand(1L, "PERCENTAGE", BigDecimal.TEN, null, null)))
            .isInstanceOf(ProductNotFoundException.class);

        verify(productDiscountRepository, never()).save(any());
    }

    @Test
    void 기존_할인이_없으면_생성한다() {
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product()));
        when(productDiscountRepository.findAllByProductId(1L)).thenReturn(List.of());
        when(productDiscountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreateProductDiscountResult result = service.create(
            new CreateProductDiscountCommand(1L, "PERCENTAGE", new BigDecimal("10"), null, null));

        assertThat(result.productId()).isEqualTo(1L);
        assertThat(result.discountType()).isEqualTo("PERCENTAGE");
    }

    @Test
    void 기간이_겹치는_활성_할인이_있으면_예외() {
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product()));
        Instant now = Instant.now();
        ProductDiscount existing = ProductDiscount.create(
            1L, "PERCENTAGE", BigDecimal.TEN, now, now.plus(5, ChronoUnit.DAYS));
        when(productDiscountRepository.findAllByProductId(1L)).thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.create(new CreateProductDiscountCommand(
            1L, "FIXED_AMOUNT", BigDecimal.TEN, now.plus(1, ChronoUnit.DAYS), now.plus(3, ChronoUnit.DAYS))))
            .isInstanceOf(OverlappingDiscountException.class);

        verify(productDiscountRepository, never()).save(any());
    }

    @Test
    void 기간이_겹치지_않으면_생성한다() {
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product()));
        Instant now = Instant.now();
        ProductDiscount past = ProductDiscount.create(
            1L, "PERCENTAGE", BigDecimal.TEN, now.minus(10, ChronoUnit.DAYS), now.minus(5, ChronoUnit.DAYS));
        when(productDiscountRepository.findAllByProductId(1L)).thenReturn(List.of(past));
        when(productDiscountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreateProductDiscountResult result = service.create(
            new CreateProductDiscountCommand(1L, "PERCENTAGE", BigDecimal.TEN, now, null));

        assertThat(result.productId()).isEqualTo(1L);
    }
}
