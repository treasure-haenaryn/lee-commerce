package com.github.haenaryn.user.interfaces;

import com.github.haenaryn.user.domain.exception.AuthenticationFailedException;
import com.github.haenaryn.user.domain.exception.DuplicateEmailException;
import com.github.haenaryn.user.domain.exception.InvalidPasswordException;
import com.github.haenaryn.user.domain.exception.InvalidRefreshTokenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "com.github.haenaryn.user.interfaces")
class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateEmailException.class)
    ResponseEntity<ApiErrorResponse> handleDuplicateEmail(DuplicateEmailException e) {
        return error(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", e.getMessage());
    }

    @ExceptionHandler(InvalidPasswordException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidPassword(InvalidPasswordException e) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD", e.getMessage());
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    ResponseEntity<ApiErrorResponse> handleAuthenticationFailed(AuthenticationFailedException e) {
        return error(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", e.getMessage());
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidRefreshToken(InvalidRefreshTokenException e) {
        return error(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", e.getMessage());
    }

    // Email VO 등 Domain VO 생성자가 형식 오류에 던지는 예외.
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
    }

    // JSON이 아예 깨져 있거나 요청 본문이 비어있는 경우 — 이것도 Bean Validation과 마찬가지로
    // 클라이언트 잘못이라 500이 아니라 400이어야 한다. 이 핸들러 없이는 Exception 포괄 핸들러가
    // 이걸 잡아서 잘못된 요청을 서버 오류로 보고하게 된다.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException e) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청 본문을 읽을 수 없다");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
            .orElse("요청 값이 올바르지 않다");
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message);
    }

    // 예상하지 못한 예외는 응답으로는 원인을 노출하지 않되(내부 구현 정보 유출 방지),
    // 로그에는 남겨서 원인 추적이 가능하게 한다.
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnexpected(Exception e) {
        log.error("처리되지 않은 예외", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 내부 오류가 발생했다");
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(code, message));
    }
}
