package com.github.haenaryn.user.application;

import java.time.Instant;
import java.util.Optional;

// Redis에 디바이스별 세션을 저장한다 — "현재 유효한 세션"의 빠른 조회/폐기용. 회전 이력의
// 감사 추적은 RefreshTokenRepository(DB)가 맡는다.
public interface SessionRepository {

    void save(Long userId, String deviceId, String refreshTokenHash, Instant expiresAt);

    void revoke(Long userId, String deviceId);

    // 재발급/로그아웃 전에 "제출된 토큰이 정말 이 디바이스의 현재 세션인지" 확인하는 용도다 —
    // 없으면 토큰만 유효하면 아무 deviceId나 대서 다른 디바이스의 세션을 덮어쓰거나 지울 수 있다.
    Optional<String> findRefreshTokenHash(Long userId, String deviceId);
}
