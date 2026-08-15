package com.github.haenaryn.user.infrastructure.security;

import com.github.haenaryn.user.domain.PasswordHash;
import com.github.haenaryn.user.domain.RawPassword;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BCryptPasswordEncoderAdapterTest {

    private final BCryptPasswordEncoderAdapter encoder = new BCryptPasswordEncoderAdapter();

    @Test
    void 인코딩한_값은_원문과_다르다() {
        PasswordHash hash = encoder.encode(new RawPassword("password123"));

        assertThat(hash.value()).isNotEqualTo("password123");
    }

    @Test
    void 같은_원문이면_matches가_참이다() {
        RawPassword rawPassword = new RawPassword("password123");
        PasswordHash hash = encoder.encode(rawPassword);

        assertThat(encoder.matches(rawPassword, hash)).isTrue();
    }

    @Test
    void 다른_원문이면_matches가_거짓이다() {
        PasswordHash hash = encoder.encode(new RawPassword("password123"));

        assertThat(encoder.matches(new RawPassword("otherpassword"), hash)).isFalse();
    }
}
