package com.github.haenaryn.user.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

    @Test
    void 올바른_형식이면_생성된다() {
        Email email = new Email("user@example.com");

        assertThat(email.value()).isEqualTo("user@example.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "user@", "@example.com", "user example.com", " "})
    void 형식이_아니면_예외(String value) {
        assertThatThrownBy(() -> new Email(value))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void null이면_예외() {
        assertThatThrownBy(() -> new Email(null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
