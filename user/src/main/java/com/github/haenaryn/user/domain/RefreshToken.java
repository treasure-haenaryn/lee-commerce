package com.github.haenaryn.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

// User와 별도 Aggregate다 — token_hash로 독립 조회되고 회전마다 insert가 잦아 User를 통째로
// 불러올 필요가 없다. userId만 갖고 User 객체 참조는 갖지 않는다.
@Entity
@Table(name = "refresh_tokens", schema = "user")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by_token_id")
    private Long replacedByTokenId;

    protected RefreshToken() {
    }

    private RefreshToken(Long userId, String tokenHash, Instant issuedAt, Instant expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    public static RefreshToken issue(Long userId, String tokenHash, Instant issuedAt, Instant expiresAt) {
        return new RefreshToken(userId, tokenHash, issuedAt, expiresAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    // 회전(rotation): 이 토큰을 폐기하면서 이걸 대체한 새 토큰의 id를 남긴다 — 재사용 탐지 등
    // 감사 추적에 쓰인다.
    public void revoke(Instant now, Long replacedByTokenId) {
        if (isRevoked()) {
            throw new IllegalStateException("이미 폐기된 토큰이다");
        }
        this.revokedAt = now;
        this.replacedByTokenId = replacedByTokenId;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Long getReplacedByTokenId() {
        return replacedByTokenId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RefreshToken other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
