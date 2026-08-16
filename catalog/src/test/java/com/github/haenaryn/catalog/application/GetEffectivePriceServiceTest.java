package com.github.haenaryn.catalog.application;

import com.github.haenaryn.catalog.domain.Product;
import com.github.haenaryn.catalog.domain.ProductDiscount;
import com.github.haenaryn.catalog.domain.ProductDiscountRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetEffectivePriceServiceTest {

    private static final Money BASE_PRICE = new Money(new BigDecimal("10000"), Currency.getInstance("KRW"));

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductDiscountRepository productDiscountRepository;

    private GetEffectivePriceService service;

    @BeforeEach
    void setUp() {
        service = new GetEffectivePriceService(productRepository, productDiscountRepository);
    }

    @Test
    void 상품이_없으면_예외() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getEffectivePrice(new GetEffectivePriceQuery(1L, "SKU-1")))
            .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void 존재하지_않는_옵션이면_예외() {
        Product product = Product.register(1L, "티셔츠", null, BASE_PRICE,
            List.of(new ProductOptionSpec("SKU-1", null, null, null)));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.getEffectivePrice(new GetEffectivePriceQuery(1L, "NOT-EXIST")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 유효한_할인이_없으면_옵션_가격_그대로_반환한다() {
        Product product = Product.register(1L, "티셔츠", null, BASE_PRICE,
            List.of(new ProductOptionSpec("SKU-1", null, null, null)));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productDiscountRepository.findAllByProductId(1L)).thenReturn(List.of());

        EffectivePriceResult result = service.getEffectivePrice(new GetEffectivePriceQuery(1L, "SKU-1"));

        assertThat(result.amount()).isEqualByComparingTo("10000");
        assertThat(result.discountApplied()).isFalse();
    }

    @Test
    void 유효한_할인이_있으면_적용된_가격을_반환한다() {
        Product product = Product.register(1L, "티셔츠", null, BASE_PRICE,
            List.of(new ProductOptionSpec("SKU-1", null, null, null)));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductDiscount discount = ProductDiscount.create(1L, "PERCENTAGE", new BigDecimal("20"), null, null);
        when(productDiscountRepository.findAllByProductId(1L)).thenReturn(List.of(discount));

        EffectivePriceResult result = service.getEffectivePrice(new GetEffectivePriceQuery(1L, "SKU-1"));

        assertThat(result.amount()).isEqualByComparingTo("8000.00");
        assertThat(result.discountApplied()).isTrue();
    }
}
