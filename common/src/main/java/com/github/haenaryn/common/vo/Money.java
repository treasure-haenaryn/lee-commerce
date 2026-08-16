package com.github.haenaryn.common.vo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

public record Money(BigDecimal amount, Currency currency) {

    public Money {
        if (amount == null || currency == null) {
            throw new IllegalArgumentException("amount와 currency는 null일 수 없다");
        }
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    public boolean isGreaterThanOrEqualTo(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) >= 0;
    }

    // 정률 할인처럼 "비율을 곱한 금액"을 계산할 때 쓴다. 소수점은 통화별 최소 단위로
    // 반올림한다(KRW/JPY는 0자리, USD는 2자리 등) — 고정 2자리로 반올림하면 0자리 통화에서
    // 잘못된 금액이 나온다.
    public Money multiply(BigDecimal factor) {
        int scale = Math.max(currency.getDefaultFractionDigits(), 0);
        return new Money(this.amount.multiply(factor).setScale(scale, RoundingMode.HALF_UP), this.currency);
    }

    private void requireSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("통화가 다른 Money끼리는 연산할 수 없다: %s vs %s"
                .formatted(this.currency, other.currency));
        }
    }
}
