package com.github.haenaryn.catalog.interfaces;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyInterceptorTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private ApiKeyInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new ApiKeyInterceptor(new ApiKeyProperties("correct-key"));
    }

    @Test
    void 올바른_키면_통과한다() {
        when(request.getHeader("X-API-KEY")).thenReturn("correct-key");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    @Test
    void 헤더가_없으면_예외() {
        when(request.getHeader("X-API-KEY")).thenReturn(null);

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
            .isInstanceOf(InvalidApiKeyException.class);
    }

    @Test
    void 키가_틀리면_예외() {
        when(request.getHeader("X-API-KEY")).thenReturn("wrong-key");

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
            .isInstanceOf(InvalidApiKeyException.class);
    }
}
