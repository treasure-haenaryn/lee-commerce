package com.github.haenaryn.catalog.domain;

import com.github.haenaryn.common.vo.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductOptionTest {

    private static final Currency KRW = Currency.getInstance("KRW");
    private static final Money BASE_PRICE = new Money(new BigDecimal("10000"), KRW);

    @Test
    void SKU_코드가_비어있으면_생성에_실패한다() {
        ProductOptionSpec spec = new ProductOptionSpec(" ", null, null, null);

        assertThatThrownBy(() -> ProductOption.from(spec))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 가격_override가_없으면_기본가를_따른다() {
        ProductOption option = ProductOption.from(new ProductOptionSpec("SKU-1", null, null, null));

        Money effectivePrice = option.effectivePrice(BASE_PRICE);

        assertThat(effectivePrice).isEqualTo(BASE_PRICE);
    }

    @Test
    void 가격_override가_음수면_생성에_실패한다() {
        Money negative = new Money(new BigDecimal("-1"), KRW);

        assertThatThrownBy(() -> ProductOption.from(new ProductOptionSpec("SKU-1", null, null, negative)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 가격_override가_있으면_그_값을_따른다() {
        Money override = new Money(new BigDecimal("8000"), KRW);
        ProductOption option = ProductOption.from(new ProductOptionSpec("SKU-1", null, null, override));

        Money effectivePrice = option.effectivePrice(BASE_PRICE);

        assertThat(effectivePrice).isEqualTo(override);
    }
}
