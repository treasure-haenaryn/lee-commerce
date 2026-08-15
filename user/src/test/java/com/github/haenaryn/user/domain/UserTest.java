package com.github.haenaryn.user.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private final Email email = new Email("user@example.com");
    private final PasswordHash passwordHash = new PasswordHash("$2a$10$hashedvalue");

    @Test
    void 가입하면_ACTIVE_상태다() {
        User user = User.register(email, passwordHash, "홍길동", "010-1234-5678");

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getName()).isEqualTo("홍길동");
        assertThat(user.getPhone()).isEqualTo("010-1234-5678");
    }

    @Test
    void 이름이_비어있으면_예외() {
        assertThatThrownBy(() -> User.register(email, passwordHash, " ", null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 이름이_null이면_예외() {
        assertThatThrownBy(() -> User.register(email, passwordHash, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
