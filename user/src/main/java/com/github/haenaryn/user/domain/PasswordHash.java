package com.github.haenaryn.user.domain;

public record PasswordHash(String value) {

    public PasswordHash {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("password hash는 비어 있을 수 없다");
        }
    }
}
