package com.github.haenaryn.user.application;

import java.time.Instant;

// Redis에 디바이스별 세션을 저장한다 — "현재 유효한 세션"의 빠른 조회/폐기용. 회전 이력의
// 감사 추적은 RefreshTokenRepository(DB)가 맡는다.
public interface SessionRepository {

    void save(Long userId, String deviceId, String refreshTokenHash, Instant expiresAt);

    void revoke(Long userId, String deviceId);
}
