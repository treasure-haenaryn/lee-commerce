package com.github.haenaryn.user.domain;

import com.github.haenaryn.user.domain.exception.InvalidPasswordException;

// NIST 800-63B 기준: 길이만 강제하고(8~64자) 대소문자/숫자/특수문자 조합은 강제하지 않는다.
// 유출 비밀번호 목록 대조는 외부 데이터 조회가 필요해 Domain에서 판단할 수 없다 — 그건
// Application/Infrastructure 단계에서 별도 포트로 검사한다.
public record RawPassword(String value) {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 64;

    public RawPassword {
        if (value == null || value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new InvalidPasswordException(
                "비밀번호는 %d자 이상 %d자 이하여야 한다".formatted(MIN_LENGTH, MAX_LENGTH));
        }
    }
}
