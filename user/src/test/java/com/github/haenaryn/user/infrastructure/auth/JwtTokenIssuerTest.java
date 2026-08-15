package com.github.haenaryn.user.infrastructure.auth;

import com.github.haenaryn.user.application.IssuedAccessToken;
import com.github.haenaryn.user.application.IssuedRefreshToken;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenIssuerTest {

    private static final String SECRET = "test-secret-key-at-least-32-bytes-long-0123456789";

    private final JwtTokenIssuer tokenIssuer = new JwtTokenIssuer(new JwtProperties(SECRET));

    @Test
    void 액세스_토큰은_사용자_id를_subject로_갖고_15분_뒤_만료된다() {
        Instant before = Instant.now();
        IssuedAccessToken token = tokenIssuer.issueAccessToken(42L);
        Instant after = Instant.now();

        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token.value()).getPayload();

        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(token.expiresAt()).isAfter(before.plusSeconds(14 * 60));
        assertThat(token.expiresAt()).isBefore(after.plusSeconds(15 * 60 + 1));
    }

    @Test
    void 리프레시_토큰은_원문의_SHA256_해시를_같이_발급한다() throws Exception {
        IssuedRefreshToken token = tokenIssuer.issueRefreshToken();

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String expectedHash = HexFormat.of().formatHex(digest.digest(token.rawValue().getBytes(StandardCharsets.UTF_8)));

        assertThat(token.hashedValue()).isEqualTo(expectedHash);
    }

    @Test
    void 리프레시_토큰은_매번_다르다() {
        IssuedRefreshToken first = tokenIssuer.issueRefreshToken();
        IssuedRefreshToken second = tokenIssuer.issueRefreshToken();

        assertThat(first.rawValue()).isNotEqualTo(second.rawValue());
    }
}
