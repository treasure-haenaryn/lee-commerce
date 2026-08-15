package com.github.haenaryn.user.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AddressTest {

    @Test
    void 등록하면_지정한_값을_그대로_갖는다() {
        Address address = Address.register(
            1L, "홍길동", "010-1234-5678", "12345", "서울시 강남구", "101동 101호", true);

        assertThat(address.getUserId()).isEqualTo(1L);
        assertThat(address.getRecipientName()).isEqualTo("홍길동");
        assertThat(address.isDefault()).isTrue();
    }

    @Test
    void 수령인_이름이_비어있으면_예외() {
        assertThatThrownBy(() -> Address.register(1L, " ", "010-1234-5678", "12345", "서울시", null, false))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 기본_배송지_지정과_해제() {
        Address address = Address.register(1L, "홍길동", "010-1234-5678", "12345", "서울시", null, false);

        address.markAsDefault();
        assertThat(address.isDefault()).isTrue();

        address.unmarkAsDefault();
        assertThat(address.isDefault()).isFalse();
    }
}
