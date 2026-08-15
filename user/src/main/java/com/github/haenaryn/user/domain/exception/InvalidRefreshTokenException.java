package com.github.haenaryn.user.domain.exception;

// 없음/만료/이미 폐기됨을 구분해서 알려주지 않는다 — 사유별로 다른 응답이 나가면
// 그 차이만으로 토큰 상태를 추측할 수 있다.
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("리프레시 토큰이 유효하지 않다");
    }
}
