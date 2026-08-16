package com.github.haenaryn.catalog.application;

import com.github.haenaryn.catalog.domain.Product;
import com.github.haenaryn.catalog.domain.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    private RegisterProductService service;

    @Test
    void 등록에_성공하면_저장된_상품_정보를_반환한다() {
        service = new RegisterProductService(productRepository);
        when(productRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterProductResult result = service.register(new RegisterProductCommand(
            1L, "티셔츠", null, new BigDecimal("10000"), "KRW",
            List.of(new ProductOptionSpecCommand("SKU-1", null, null, null, null))));

        assertThat(result.name()).isEqualTo("티셔츠");
        assertThat(result.status()).isEqualTo("ON_SALE");
        assertThat(result.skuCodes()).containsExactly("SKU-1");
    }

    @Test
    void 옵션별_가격_override가_있으면_반영된_상품이_저장된다() {
        service = new RegisterProductService(productRepository);
        when(productRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterProductResult result = service.register(new RegisterProductCommand(
            1L, "티셔츠", null, new BigDecimal("10000"), "KRW",
            List.of(
                new ProductOptionSpecCommand("SKU-1", "L", "BLACK", new BigDecimal("12000"), "KRW"),
                new ProductOptionSpecCommand("SKU-2", "M", "WHITE", null, null))));

        assertThat(result.skuCodes()).containsExactlyInAnyOrder("SKU-1", "SKU-2");
    }
}
