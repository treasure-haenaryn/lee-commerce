package com.github.haenaryn.catalog.domain;

import com.github.haenaryn.common.vo.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Currency;

// Product의 자식 Entity다 — 재고/장바구니/주문은 Product가 아니라 이 옵션(SKU)을 참조한다.
// 부모(Product)를 가리키는 필드는 두지 않는다(캡슐화) — product_id 컬럼은 Product 쪽
// @OneToMany의 @JoinColumn이 관리한다.
@Entity
@Table(name = "product_options", schema = "catalog")
public class ProductOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sku_code", nullable = false, unique = true)
    private String skuCode;

    @Column(name = "size")
    private String size;

    @Column(name = "color")
    private String color;

    @Column(name = "price_override_amount")
    private BigDecimal priceOverrideAmount;

    @Column(name = "price_override_currency")
    private String priceOverrideCurrency;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    protected ProductOption() {
    }

    ProductOption(String skuCode, String size, String color, Money priceOverride) {
        if (skuCode == null || skuCode.isBlank()) {
            throw new IllegalArgumentException("SKU 코드는 비어 있을 수 없다");
        }
        if (priceOverride != null && priceOverride.amount().signum() < 0) {
            throw new IllegalArgumentException("가격 override는 0 이상이어야 한다");
        }
        this.skuCode = skuCode;
        this.size = size;
        this.color = color;
        if (priceOverride != null) {
            this.priceOverrideAmount = priceOverride.amount();
            this.priceOverrideCurrency = priceOverride.currency().getCurrencyCode();
        }
        this.isActive = true;
    }

    static ProductOption from(ProductOptionSpec spec) {
        return new ProductOption(spec.skuCode(), spec.size(), spec.color(), spec.priceOverride());
    }

    public void deactivate() {
        this.isActive = false;
    }

    public Money effectivePrice(Money basePrice) {
        if (priceOverrideAmount == null) {
            return basePrice;
        }
        return new Money(priceOverrideAmount, Currency.getInstance(priceOverrideCurrency));
    }

    public Long getId() {
        return id;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public String getSize() {
        return size;
    }

    public String getColor() {
        return color;
    }

    public boolean isActive() {
        return isActive;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProductOption other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
