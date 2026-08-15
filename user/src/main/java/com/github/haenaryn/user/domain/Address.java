package com.github.haenaryn.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

// User와 별도 Aggregate다 — 저장/수정 트랜잭션 단위가 User와 다르다. userId만 갖는다.
@Entity
@Table(name = "addresses", schema = "user")
@EntityListeners(AuditingEntityListener.class)
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "recipient_name", nullable = false)
    private String recipientName;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "zip_code", nullable = false)
    private String zipCode;

    @Column(name = "address_line1", nullable = false)
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Address() {
    }

    private Address(Long userId, String recipientName, String phone, String zipCode,
                     String addressLine1, String addressLine2, boolean isDefault) {
        this.userId = userId;
        this.recipientName = recipientName;
        this.phone = phone;
        this.zipCode = zipCode;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.isDefault = isDefault;
    }

    public static Address register(Long userId, String recipientName, String phone, String zipCode,
                                    String addressLine1, String addressLine2, boolean isDefault) {
        if (recipientName == null || recipientName.isBlank()) {
            throw new IllegalArgumentException("수령인 이름은 비어 있을 수 없다");
        }
        return new Address(userId, recipientName, phone, zipCode, addressLine1, addressLine2, isDefault);
    }

    // 사용자당 기본 배송지는 하나만 존재할 수 있다(DB에 partial unique index로도 강제됨).
    // 여러 주소 중 하나만 기본으로 유지되도록 나머지를 해제하는 조율은 Application 책임 — 여기서는
    // "이 로우 하나의 상태 전이"만 다룬다.
    public void markAsDefault() {
        this.isDefault = true;
    }

    public void unmarkAsDefault() {
        this.isDefault = false;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getPhone() {
        return phone;
    }

    public String getZipCode() {
        return zipCode;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public boolean isDefault() {
        return isDefault;
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
        if (!(o instanceof Address other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
