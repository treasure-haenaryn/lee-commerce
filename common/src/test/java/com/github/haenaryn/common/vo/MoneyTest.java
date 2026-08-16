package com.github.haenaryn.common.vo;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyTest {

    private static final Currency KRW = Currency.getInstance("KRW");
    private static final Currency USD = Currency.getInstance("USD");

    @Test
    void KRW는_소수_자릿수_없이_반올림된다() {
        Money price = new Money(new BigDecimal("10000"), KRW);

        Money result = price.multiply(new BigDecimal("0.335"));

        assertThat(result.amount()).isEqualByComparingTo("3350");
        assertThat(result.amount().scale()).isZero();
    }

    @Test
    void USD는_소수_둘째_자리에서_반올림된다() {
        Money price = new Money(new BigDecimal("100"), USD);

        Money result = price.multiply(new BigDecimal("0.333"));

        assertThat(result.amount()).isEqualByComparingTo("33.30");
        assertThat(result.amount().scale()).isEqualTo(2);
    }

    @Test
    void JPY는_소수_자릿수_없이_반올림된다() {
        Money price = new Money(new BigDecimal("1000"), Currency.getInstance("JPY"));

        Money result = price.multiply(new BigDecimal("0.335"));

        assertThat(result.amount()).isEqualByComparingTo("335");
        assertThat(result.amount().scale()).isZero();
    }
}
