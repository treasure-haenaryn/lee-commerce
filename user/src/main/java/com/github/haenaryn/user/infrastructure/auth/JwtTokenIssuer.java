package com.github.haenaryn.user.infrastructure.auth;

import com.github.haenaryn.user.application.IssuedAccessToken;
import com.github.haenaryn.user.application.IssuedRefreshToken;
import com.github.haenaryn.user.application.TokenIssuer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;

@Component
@RequiredArgsConstructor
class JwtTokenIssuer implements TokenIssuer {

    // ADR로 이미 정해진 값(15분/14일)이라 설정이 아니라 코드 상수로 둔다.
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(14);
    private static final int REFRESH_TOKEN_BYTE_LENGTH = 32;

    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public IssuedAccessToken issueAccessToken(Long userId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ACCESS_TOKEN_TTL);

        String token = Jwts.builder()
            .subject(userId.toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .signWith(signingKey())
            .compact();

        return new IssuedAccessToken(token, expiresAt);
    }

    @Override
    public IssuedRefreshToken issueRefreshToken() {
        byte[] randomBytes = new byte[REFRESH_TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(randomBytes);
        String rawValue = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        return new IssuedRefreshToken(rawValue, hash(rawValue), Instant.now().plus(REFRESH_TOKEN_TTL));
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    // 리프레시 토큰은 이미 256비트 랜덤값이라 브루트포스 대상이 아니다 — 비밀번호처럼 느린
    // 해시(BCrypt)가 필요 없고, 조회용으로 빠른 SHA-256이면 충분하다.
    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없다", e);
        }
    }
}
