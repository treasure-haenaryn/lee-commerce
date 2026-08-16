package com.github.haenaryn.catalog.application;

import com.github.haenaryn.catalog.domain.ProductDiscount;
import com.github.haenaryn.catalog.domain.ProductDiscountRepository;
import com.github.haenaryn.catalog.domain.exception.ProductDiscountNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeactivateProductDiscountServiceTest {

    @Mock
    private ProductDiscountRepository productDiscountRepository;

    private DeactivateProductDiscountService service;

    @BeforeEach
    void setUp() {
        service = new DeactivateProductDiscountService(productDiscountRepository);
    }

    @Test
    void 할인이_없으면_예외() {
        when(productDiscountRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivate(new DeactivateProductDiscountCommand(1L)))
            .isInstanceOf(ProductDiscountNotFoundException.class);

        verify(productDiscountRepository, never()).save(any());
    }

    @Test
    void 비활성화하면_저장한다() {
        ProductDiscount discount = ProductDiscount.create(1L, "PERCENTAGE", BigDecimal.TEN, null, null);
        when(productDiscountRepository.findById(1L)).thenReturn(Optional.of(discount));

        service.deactivate(new DeactivateProductDiscountCommand(1L));

        assertThat(discount.isActive()).isFalse();
        verify(productDiscountRepository).save(discount);
    }
}
