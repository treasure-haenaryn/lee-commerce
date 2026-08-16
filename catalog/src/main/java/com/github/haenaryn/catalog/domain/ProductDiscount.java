package com.github.haenaryn.catalog.domain;

import com.github.haenaryn.common.vo.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

// Product와 별도 Aggregate다 — 상품에 할인이 없어도 유효한 상태라 ProductOption처럼
// "반드시 존재해야 하는" 불변식이 없다. productId만 갖고 Product 객체 참조는 갖지 않는다.
@Entity
@Table(name = "product_discounts", schema = "catalog")
public class ProductDiscount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "discount_type", nullable = false)
    private String discountType;

    @Column(name = "discount_value", nullable = false)
    private BigDecimal discountValue;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    protected ProductDiscount() {
    }

    private ProductDiscount(Long productId, String discountType, BigDecimal discountValue,
                             Instant startsAt, Instant endsAt) {
        this.productId = productId;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.isActive = true;
    }

    private static final Set<String> SUPPORTED_DISCOUNT_TYPES = Set.of("PERCENTAGE", "FIXED_AMOUNT");

    public static ProductDiscount create(
            Long productId, String discountType, BigDecimal discountValue, Instant startsAt, Instant endsAt) {
        if (productId == null) {
            throw new IllegalArgumentException("상품 ID는 비어 있을 수 없다");
        }
        if (!SUPPORTED_DISCOUNT_TYPES.contains(discountType)) {
            throw new IllegalArgumentException("지원하지 않는 할인 유형이다: " + discountType);
        }
        if (discountValue == null || discountValue.signum() <= 0) {
            throw new IllegalArgumentException("할인값은 0보다 커야 한다");
        }
        if ("PERCENTAGE".equals(discountType) && discountValue.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("정률 할인값은 100을 초과할 수 없다");
        }
        if (startsAt != null && endsAt != null && !startsAt.isBefore(endsAt)) {
            throw new IllegalArgumentException("시작 시각은 종료 시각보다 앞서야 한다");
        }
        return new ProductDiscount(productId, discountType, discountValue, startsAt, endsAt);
    }

    public void deactivate() {
        this.isActive = false;
    }

    // starts_at/ends_at이 null이면 그 방향으로는 기간 제한이 없다는 뜻이다.
    public boolean isEffective(Instant now) {
        if (!isActive) {
            return false;
        }
        if (startsAt != null && now.isBefore(startsAt)) {
            return false;
        }
        return endsAt == null || !now.isAfter(endsAt);
    }

    // discountType에 따른 분기는 "할인 적용"이라는 하나의 책임 안의 변형이다 — 새 계산
    // 방식이 필요하면 여기 case만 늘리면 되고 DB 마이그레이션은 필요 없다(discount_type에
    // CHECK 제약을 일부러 안 걸어둔 이유). 정액 할인값은 원가와 무관하게 생성되므로 여기서
    // 결과가 0 미만이 되지 않도록 바닥을 둔다 — 판매가가 음수가 되는 것을 막는다.
    public Money applyTo(Money originalPrice) {
        Money discounted = switch (discountType) {
            case "PERCENTAGE" -> {
                BigDecimal factor = BigDecimal.ONE.subtract(
                    discountValue.movePointLeft(2));
                yield originalPrice.multiply(factor);
            }
            case "FIXED_AMOUNT" -> originalPrice.minus(new Money(discountValue, originalPrice.currency()));
            default -> throw new IllegalStateException("알 수 없는 할인 유형이다: " + discountType);
        };
        Money zero = Money.zero(originalPrice.currency());
        return discounted.isGreaterThanOrEqualTo(zero) ? discounted : zero;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public String getDiscountType() {
        return discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public boolean isActive() {
        return isActive;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProductDiscount other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
