package com.github.haenaryn.catalog.interfaces;

import com.github.haenaryn.catalog.domain.exception.CategoryNotFoundException;
import com.github.haenaryn.catalog.domain.exception.OverlappingDiscountException;
import com.github.haenaryn.catalog.domain.exception.ProductDiscountNotFoundException;
import com.github.haenaryn.catalog.domain.exception.ProductNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice(basePackages = "com.github.haenaryn.catalog.interfaces")
class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleProductNotFound(ProductNotFoundException e) {
        return error(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleCategoryNotFound(CategoryNotFoundException e) {
        return error(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(ProductDiscountNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleDiscountNotFound(ProductDiscountNotFoundException e) {
        return error(HttpStatus.NOT_FOUND, "DISCOUNT_NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(OverlappingDiscountException.class)
    ResponseEntity<ApiErrorResponse> handleOverlappingDiscount(OverlappingDiscountException e) {
        return error(HttpStatus.CONFLICT, "OVERLAPPING_DISCOUNT", e.getMessage());
    }

    @ExceptionHandler(InvalidApiKeyException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidApiKey(InvalidApiKeyException e) {
        return error(HttpStatus.UNAUTHORIZED, "INVALID_API_KEY", e.getMessage());
    }

    // Product.deactivateOption의 "마지막 남은 활성 옵션은 비활성화할 수 없다" 같은,
    // 현재 상태와 충돌하는 요청.
    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ApiErrorResponse> handleIllegalState(IllegalStateException e) {
        return error(HttpStatus.CONFLICT, "CONFLICT", e.getMessage());
    }

    // Domain 생성자/Application 검증이 던지는 예외(가격 범위, 상품 이름 등).
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
    }

    // path/query 파라미터가 선언된 타입(Long, int, BigDecimal 등)으로 변환되지 않을 때
    // 발생한다 — 예: /internal/products/abc/status. 이 핸들러가 없으면 아래 Exception
    // 포괄 핸들러가 잡아서 클라이언트 잘못을 500으로 보고하게 된다.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
            "%s 파라미터 값이 올바르지 않다: %s".formatted(e.getName(), e.getValue()));
    }

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
