package com.github.haenaryn.user.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordHashTest {

    @Test
    void 값이_있으면_생성된다() {
        PasswordHash hash = new PasswordHash("$2a$10$hashedvalue");

        assertThat(hash.value()).isEqualTo("$2a$10$hashedvalue");
    }

    @Test
    void 빈값이면_예외() {
        assertThatThrownBy(() -> new PasswordHash(" "))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void null이면_예외() {
        assertThatThrownBy(() -> new PasswordHash(null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
