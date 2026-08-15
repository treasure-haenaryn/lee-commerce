package com.github.haenaryn.catalog.domain;

import com.github.haenaryn.common.vo.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductDiscountTest {

    private static final Currency KRW = Currency.getInstance("KRW");
    private static final Money ORIGINAL_PRICE = new Money(new BigDecimal("10000"), KRW);

    @Test
    void 할인값이_0이하면_생성에_실패한다() {
        assertThatThrownBy(() ->
            ProductDiscount.create(1L, "PERCENTAGE", BigDecimal.ZERO, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 상품_ID가_없으면_생성에_실패한다() {
        assertThatThrownBy(() ->
            ProductDiscount.create(null, "PERCENTAGE", BigDecimal.TEN, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 지원하지_않는_할인_유형이면_생성에_실패한다() {
        assertThatThrownBy(() ->
            ProductDiscount.create(1L, "BOGO", BigDecimal.TEN, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 정률_할인값이_100을_초과하면_생성에_실패한다() {
        assertThatThrownBy(() ->
            ProductDiscount.create(1L, "PERCENTAGE", new BigDecimal("101"), null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 시작_시각이_종료_시각보다_늦으면_생성에_실패한다() {
        Instant now = Instant.now();

        assertThatThrownBy(() ->
            ProductDiscount.create(1L, "PERCENTAGE", BigDecimal.TEN, now, now.minus(1, ChronoUnit.DAYS)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 정률_할인을_적용한다() {
        ProductDiscount discount = ProductDiscount.create(1L, "PERCENTAGE", new BigDecimal("20"), null, null);

        Money result = discount.applyTo(ORIGINAL_PRICE);

        assertThat(result.amount()).isEqualByComparingTo("8000.00");
    }

    @Test
    void 정액_할인을_적용한다() {
        ProductDiscount discount = ProductDiscount.create(1L, "FIXED_AMOUNT", new BigDecimal("1500"), null, null);

        Money result = discount.applyTo(ORIGINAL_PRICE);

        assertThat(result.amount()).isEqualByComparingTo("8500");
    }

    @Test
    void 정액_할인이_원가보다_크면_0원으로_바닥을_둔다() {
        ProductDiscount discount = ProductDiscount.create(1L, "FIXED_AMOUNT", new BigDecimal("50000"), null, null);

        Money result = discount.applyTo(ORIGINAL_PRICE);

        assertThat(result.amount()).isEqualByComparingTo("0");
    }

    @Test
    void 기간_제한이_없고_활성이면_유효하다() {
        ProductDiscount discount = ProductDiscount.create(1L, "PERCENTAGE", BigDecimal.TEN, null, null);

        assertThat(discount.isEffective(Instant.now())).isTrue();
    }

    @Test
    void 시작_전이면_유효하지_않다() {
        Instant now = Instant.now();
        ProductDiscount discount = ProductDiscount.create(
            1L, "PERCENTAGE", BigDecimal.TEN, now.plus(1, ChronoUnit.DAYS), null);

        assertThat(discount.isEffective(now)).isFalse();
    }

    @Test
    void 종료_후면_유효하지_않다() {
        Instant now = Instant.now();
        ProductDiscount discount = ProductDiscount.create(
            1L, "PERCENTAGE", BigDecimal.TEN, now.minus(2, ChronoUnit.DAYS), now.minus(1, ChronoUnit.DAYS));

        assertThat(discount.isEffective(now)).isFalse();
    }

    @Test
    void 비활성이면_기간_안이어도_유효하지_않다() {
        ProductDiscount discount = ProductDiscount.create(1L, "PERCENTAGE", BigDecimal.TEN, null, null);
        discount.deactivate();

        assertThat(discount.isEffective(Instant.now())).isFalse();
    }
}
