package com.github.haenaryn.inventory.domain;

import com.github.haenaryn.inventory.domain.exception.InsufficientStockException;
import com.github.haenaryn.inventory.domain.exception.StockReservationQuantityMismatchException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

// 재고는 상품이 아니라 상품 옵션(SKU) 단위로 관리한다 — productOptionId만 갖고
// catalog.ProductOption 객체 참조는 갖지 않는다(스키마 간 물리적 FK 없음).
@Entity
@Table(name = "stocks", schema = "inventory")
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_option_id", nullable = false, unique = true)
    private Long productOptionId;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    // 낙관적 락(기본 전략) — 비관적 락/Redis 분산락 전략을 쓰는 경로에서도 엔티티
    // 자체에는 항상 존재해 매 UPDATE마다 증가한다.
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected Stock() {
    }

    private Stock(Long productOptionId, int availableQuantity) {
        this.productOptionId = productOptionId;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = 0;
    }

    public static Stock register(Long productOptionId, int initialQuantity) {
        if (productOptionId == null) {
            throw new IllegalArgumentException("상품 옵션 ID는 비어 있을 수 없다");
        }
        if (initialQuantity < 0) {
            throw new IllegalArgumentException("초기 수량은 0 이상이어야 한다");
        }
        return new Stock(productOptionId, initialQuantity);
    }

    public void reserve(int quantity) {
        validatePositive(quantity);
        if (availableQuantity < quantity) {
            throw new InsufficientStockException(productOptionId, quantity, availableQuantity);
        }
        availableQuantity -= quantity;
        reservedQuantity += quantity;
    }

    // 결제 승인 시 호출 — 가용 수량은 예약 시점에 이미 빠졌으므로 예약 수량만 정리한다.
    public void confirmReservation(int quantity) {
        validatePositive(quantity);
        if (reservedQuantity < quantity) {
            throw new StockReservationQuantityMismatchException(productOptionId, quantity, reservedQuantity);
        }
        reservedQuantity -= quantity;
    }

    // 결제 만료/실패/취소 시 호출 — 예약을 취소하고 가용 수량으로 되돌린다.
    public void releaseReservation(int quantity) {
        validatePositive(quantity);
        if (reservedQuantity < quantity) {
            throw new StockReservationQuantityMismatchException(productOptionId, quantity, reservedQuantity);
        }
        reservedQuantity -= quantity;
        availableQuantity += quantity;
    }

    private void validatePositive(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 0보다 커야 한다: " + quantity);
        }
    }

    public Long getId() {
        return id;
    }

    public Long getProductOptionId() {
        return productOptionId;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }

    public Long getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Stock other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
