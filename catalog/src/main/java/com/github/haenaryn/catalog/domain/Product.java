package com.github.haenaryn.catalog.domain;

import com.github.haenaryn.common.vo.Money;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

// Product가 Aggregate Root다 — 옵션 없는 단순 상품도 옵션 1개로 등록한다는 불변식이 있어서
// (재고/장바구니/주문은 항상 옵션 단위로 참조), Product와 ProductOption을 같은 Aggregate로
// 묶고 생성 시점에 옵션 ≥1개를 강제한다.
@Entity
@Table(name = "products", schema = "catalog")
@EntityListeners(AuditingEntityListener.class)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "base_price_amount", nullable = false)
    private BigDecimal basePriceAmount;

    @Column(name = "base_price_currency", nullable = false)
    private String basePriceCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProductStatus status;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "product_id", nullable = false)
    private List<ProductOption> options = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Product() {
    }

    private Product(Long categoryId, String name, String description, Money basePrice) {
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.basePriceAmount = basePrice.amount();
        this.basePriceCurrency = basePrice.currency().getCurrencyCode();
        this.status = ProductStatus.ON_SALE;
    }

    public static Product register(
            Long categoryId, String name, String description, Money basePrice, List<ProductOptionSpec> optionSpecs) {
        if (categoryId == null) {
            throw new IllegalArgumentException("카테고리는 비어 있을 수 없다");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("상품 이름은 비어 있을 수 없다");
        }
        if (basePrice == null || basePrice.amount().signum() < 0) {
            throw new IllegalArgumentException("기본가는 0 이상이어야 한다");
        }
        if (optionSpecs == null || optionSpecs.isEmpty()) {
            throw new IllegalArgumentException("상품은 옵션이 최소 1개 있어야 한다");
        }

        Product product = new Product(categoryId, name, description, basePrice);
        for (ProductOptionSpec spec : optionSpecs) {
            product.addOption(spec);
        }
        return product;
    }

    // sku_code는 schema.sql에서 전역 UNIQUE라 DB가 최종 방어선이지만, 같은 Aggregate
    // 안에서의 중복은 Product 스스로 막을 수 있는 불변식이라 여기서 먼저 거른다.
    public void addOption(ProductOptionSpec spec) {
        boolean duplicate = options.stream()
            .anyMatch(option -> option.getSkuCode().equals(spec.skuCode()));
        if (duplicate) {
            throw new IllegalArgumentException("이미 존재하는 SKU 코드다: " + spec.skuCode());
        }
        options.add(ProductOption.from(spec));
    }

    // 마지막 남은 옵션을 비활성화하면 상품이 "팔 수 있는 옵션이 0개"인 상태가 되어 등록 시점의
    // 불변식(옵션 ≥1개)이 깨진다.
    public void deactivateOption(String skuCode) {
        ProductOption target = findOption(skuCode);
        long activeCount = options.stream().filter(ProductOption::isActive).count();
        if (activeCount <= 1 && target.isActive()) {
            throw new IllegalStateException("마지막 남은 활성 옵션은 비활성화할 수 없다");
        }
        target.deactivate();
    }

    public void changeStatus(ProductStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("상태는 비어 있을 수 없다");
        }
        this.status = newStatus;
    }

    private ProductOption findOption(String skuCode) {
        return options.stream()
            .filter(option -> option.getSkuCode().equals(skuCode))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 옵션이다: " + skuCode));
    }

    public Long getId() {
        return id;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Money getBasePrice() {
        return new Money(basePriceAmount, Currency.getInstance(basePriceCurrency));
    }

    public ProductStatus getStatus() {
        return status;
    }

    public List<ProductOption> getOptions() {
        return List.copyOf(options);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Product other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
