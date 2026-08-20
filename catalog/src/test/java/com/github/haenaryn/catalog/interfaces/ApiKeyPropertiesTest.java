package com.github.haenaryn.catalog.interfaces;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiKeyPropertiesTest {

    @Test
    void 비어있으면_예외() {
        assertThatThrownBy(() -> new ApiKeyProperties(" "))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void null이면_예외() {
        assertThatThrownBy(() -> new ApiKeyProperties(null))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 값이_있으면_정상_생성된다() {
        ApiKeyProperties properties = new ApiKeyProperties("secret-key");

        assertThat(properties.value()).isEqualTo("secret-key");
    }
}
