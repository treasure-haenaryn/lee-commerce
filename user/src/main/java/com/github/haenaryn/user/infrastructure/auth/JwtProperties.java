package com.github.haenaryn.user.infrastructure.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret) {

    // HS256 서명키는 최소 256비트(32바이트)가 아니면 jjwt가 WeakKeyException을 던진다 —
    // 여기서 먼저 검증해서 애플리케이션 기동 시점에 바로 실패하게 한다(런타임 토큰 발급
    // 시점까지 미루지 않음).
    private static final int MIN_SECRET_BYTE_LENGTH = 32;

    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("jwt.secret이 설정되지 않았다 — JWT_SECRET 환경변수를 지정해야 한다");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTE_LENGTH) {
            throw new IllegalStateException("jwt.secret은 최소 %d바이트 이상이어야 한다".formatted(MIN_SECRET_BYTE_LENGTH));
        }
    }
}
