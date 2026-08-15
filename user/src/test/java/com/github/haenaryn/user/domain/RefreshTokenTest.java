package com.github.haenaryn.user.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshTokenTest {

    @Test
    void 발급하면_폐기되지_않은_상태다() {
        Instant now = Instant.now();
        RefreshToken token = RefreshToken.issue(1L, "hash", now, now.plus(14, ChronoUnit.DAYS));

        assertThat(token.isRevoked()).isFalse();
        assertThat(token.getUserId()).isEqualTo(1L);
    }

    @Test
    void 만료_시각이_지나면_만료된_것이다() {
        Instant issuedAt = Instant.now().minus(15, ChronoUnit.DAYS);
        RefreshToken token = RefreshToken.issue(1L, "hash", issuedAt, issuedAt.plus(14, ChronoUnit.DAYS));

        assertThat(token.isExpired(Instant.now())).isTrue();
    }

    @Test
    void 회전하면_폐기되고_대체_토큰_id가_남는다() {
        Instant now = Instant.now();
        RefreshToken token = RefreshToken.issue(1L, "hash", now, now.plus(14, ChronoUnit.DAYS));

        token.revoke(now, 99L);

        assertThat(token.isRevoked()).isTrue();
        assertThat(token.getReplacedByTokenId()).isEqualTo(99L);
    }

    @Test
    void 이미_폐기된_토큰을_다시_폐기하면_예외() {
        Instant now = Instant.now();
        RefreshToken token = RefreshToken.issue(1L, "hash", now, now.plus(14, ChronoUnit.DAYS));
        token.revoke(now, 99L);

        assertThatThrownBy(() -> token.revoke(now, 100L))
            .isInstanceOf(IllegalStateException.class);
    }
}
