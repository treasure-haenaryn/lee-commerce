package com.github.haenaryn.user.domain.exception;

// 이메일이 없는 경우와 비밀번호가 틀린 경우를 구분해서 알려주지 않는다 — 계정 존재 여부가
// 유출되면 이메일 목록 대조 공격에 악용될 수 있다.
public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException() {
        super("이메일 또는 비밀번호가 올바르지 않다");
    }
}
