package com.github.haenaryn.user.interfaces;

import com.github.haenaryn.user.domain.exception.AuthenticationFailedException;
import com.github.haenaryn.user.domain.exception.DuplicateEmailException;
import com.github.haenaryn.user.domain.exception.InvalidPasswordException;
import com.github.haenaryn.user.domain.exception.InvalidRefreshTokenException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void 이메일_중복은_409() {
        ResponseEntity<ApiErrorResponse> response = handler.handleDuplicateEmail(
            new DuplicateEmailException("user@example.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code()).isEqualTo("DUPLICATE_EMAIL");
    }

    @Test
    void 비밀번호_규칙_위반은_400() {
        ResponseEntity<ApiErrorResponse> response = handler.handleInvalidPassword(
            new InvalidPasswordException("too short"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("INVALID_PASSWORD");
    }

    @Test
    void 인증_실패는_401() {
        ResponseEntity<ApiErrorResponse> response = handler.handleAuthenticationFailed(
            new AuthenticationFailedException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().code()).isEqualTo("AUTHENTICATION_FAILED");
    }

    @Test
    void 유효하지_않은_리프레시_토큰은_401() {
        ResponseEntity<ApiErrorResponse> response = handler.handleInvalidRefreshToken(
            new InvalidRefreshTokenException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().code()).isEqualTo("INVALID_REFRESH_TOKEN");
    }

    @Test
    void 잘못된_인자는_400() {
        ResponseEntity<ApiErrorResponse> response = handler.handleIllegalArgument(
            new IllegalArgumentException("이메일 형식이 아니다"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("INVALID_REQUEST");
    }

    @Test
    void 깨진_JSON_요청은_500이_아니라_400() {
        ResponseEntity<ApiErrorResponse> response = handler.handleMessageNotReadable(
            new HttpMessageNotReadableException("broken json", (HttpInputMessage) null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("INVALID_REQUEST");
    }

    @Test
    void 예상하지_못한_예외는_원인을_노출하지_않고_500() {
        ResponseEntity<ApiErrorResponse> response = handler.handleUnexpected(
            new RuntimeException("내부 구현 세부사항"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).doesNotContain("내부 구현 세부사항");
    }
}
