package com.github.haenaryn.catalog.interfaces;

import com.github.haenaryn.catalog.domain.exception.CategoryNotFoundException;
import com.github.haenaryn.catalog.domain.exception.OverlappingDiscountException;
import com.github.haenaryn.catalog.domain.exception.ProductDiscountNotFoundException;
import com.github.haenaryn.catalog.domain.exception.ProductNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void 상품_없음은_404() {
        ResponseEntity<ApiErrorResponse> response = handler.handleProductNotFound(new ProductNotFoundException(1L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().code()).isEqualTo("PRODUCT_NOT_FOUND");
    }

    @Test
    void 카테고리_없음은_404() {
        ResponseEntity<ApiErrorResponse> response =
            handler.handleCategoryNotFound(new CategoryNotFoundException(1L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().code()).isEqualTo("CATEGORY_NOT_FOUND");
    }

    @Test
    void 할인_없음은_404() {
        ResponseEntity<ApiErrorResponse> response =
            handler.handleDiscountNotFound(new ProductDiscountNotFoundException(1L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().code()).isEqualTo("DISCOUNT_NOT_FOUND");
    }

    @Test
    void 할인_기간_중복은_409() {
        ResponseEntity<ApiErrorResponse> response =
            handler.handleOverlappingDiscount(new OverlappingDiscountException(1L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code()).isEqualTo("OVERLAPPING_DISCOUNT");
    }

    @Test
    void API_Key_불일치는_401() {
        ResponseEntity<ApiErrorResponse> response = handler.handleInvalidApiKey(new InvalidApiKeyException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().code()).isEqualTo("INVALID_API_KEY");
    }

    @Test
    void 상태_충돌은_409() {
        ResponseEntity<ApiErrorResponse> response =
            handler.handleIllegalState(new IllegalStateException("마지막 남은 활성 옵션은 비활성화할 수 없다"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code()).isEqualTo("CONFLICT");
    }

    @Test
    void 잘못된_인자는_400() {
        ResponseEntity<ApiErrorResponse> response =
            handler.handleIllegalArgument(new IllegalArgumentException("가격 범위가 올바르지 않다"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("INVALID_REQUEST");
    }

    @Test
    void 파라미터_타입_불일치는_500이_아니라_400() {
        MethodArgumentTypeMismatchException e = mock(MethodArgumentTypeMismatchException.class);
        when(e.getName()).thenReturn("productId");
        when(e.getValue()).thenReturn("abc");

        ResponseEntity<ApiErrorResponse> response = handler.handleTypeMismatch(e);

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
