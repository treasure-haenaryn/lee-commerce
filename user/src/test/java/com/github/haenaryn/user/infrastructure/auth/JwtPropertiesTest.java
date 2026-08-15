package com.github.haenaryn.user.infrastructure.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

    @Test
    void 비어있으면_예외() {
        assertThatThrownBy(() -> new JwtProperties(" "))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void null이면_예외() {
        assertThatThrownBy(() -> new JwtProperties(null))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 길이가_32바이트_미만이면_예외() {
        assertThatThrownBy(() -> new JwtProperties("short-secret"))
            .isInstanceOf(IllegalStateException.class);
    }
}
