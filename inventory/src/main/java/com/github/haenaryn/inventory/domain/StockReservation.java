package com.github.haenaryn.inventory.domain;

import com.github.haenaryn.inventory.domain.exception.InvalidStockReservationStateException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

// Stock과 별도 Aggregate다 — 확정/해제를 예약 이력 근거로 멱등 처리하기 위한 감사
// 기록이라 Stock에 종속시키지 않는다. stockId만 갖고 Stock 객체 참조는 갖지 않는다.
@Entity
@Table(name = "stock_reservations", schema = "inventory")
@EntityListeners(AuditingEntityListener.class)
public class StockReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_id", nullable = false)
    private Long stockId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StockReservationStatus status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StockReservation() {
    }

    private StockReservation(Long stockId, Long orderId, int quantity) {
        this.stockId = stockId;
        this.orderId = orderId;
        this.quantity = quantity;
        this.status = StockReservationStatus.RESERVED;
    }

    public static StockReservation create(Long stockId, Long orderId, int quantity) {
        if (stockId == null) {
            throw new IllegalArgumentException("재고 ID는 비어 있을 수 없다");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("주문 ID는 비어 있을 수 없다");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 0보다 커야 한다: " + quantity);
        }
        return new StockReservation(stockId, orderId, quantity);
    }

    public void confirm() {
        if (status != StockReservationStatus.RESERVED) {
            throw new InvalidStockReservationStateException(id, status);
        }
        this.status = StockReservationStatus.CONFIRMED;
    }

    public void release() {
        if (status != StockReservationStatus.RESERVED) {
            throw new InvalidStockReservationStateException(id, status);
        }
        this.status = StockReservationStatus.RELEASED;
    }

    public Long getId() {
        return id;
    }

    public Long getStockId() {
        return stockId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public int getQuantity() {
        return quantity;
    }

    public StockReservationStatus getStatus() {
        return status;
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
        if (!(o instanceof StockReservation other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
