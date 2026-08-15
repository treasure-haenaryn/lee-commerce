package com.github.haenaryn.user.domain;

import com.github.haenaryn.user.domain.exception.InvalidPasswordException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RawPasswordTest {

    @Test
    void 최소_길이_8자는_통과한다() {
        RawPassword password = new RawPassword("a".repeat(8));

        assertThat(password.value()).hasSize(8);
    }

    @Test
    void 최대_길이_64자는_통과한다() {
        RawPassword password = new RawPassword("a".repeat(64));

        assertThat(password.value()).hasSize(64);
    }

    @Test
    void 길이가_7자면_예외() {
        assertThatThrownBy(() -> new RawPassword("a".repeat(7)))
            .isInstanceOf(InvalidPasswordException.class);
    }

    @Test
    void 길이가_65자면_예외() {
        assertThatThrownBy(() -> new RawPassword("a".repeat(65)))
            .isInstanceOf(InvalidPasswordException.class);
    }

    @Test
    void null이면_예외() {
        assertThatThrownBy(() -> new RawPassword(null))
            .isInstanceOf(InvalidPasswordException.class);
    }
}
