package com.github.haenaryn.user.domain;

import java.util.regex.Pattern;

public record Email(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public Email {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("이메일은 비어 있을 수 없다");
        }
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("이메일 형식이 아니다: " + value);
        }
    }
}
