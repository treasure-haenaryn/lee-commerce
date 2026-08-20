package com.github.haenaryn.catalog.interfaces;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
@RequiredArgsConstructor
class ApiKeyInterceptor implements HandlerInterceptor {

    private static final String HEADER_NAME = "X-API-KEY";

    private final ApiKeyProperties apiKeyProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String presented = request.getHeader(HEADER_NAME);
        if (presented == null || !isEqual(presented, apiKeyProperties.value())) {
            throw new InvalidApiKeyException();
        }
        return true;
    }

    // 문자열 길이만큼 시간이 걸리는 String.equals() 대신 타이밍 사이드채널 공격을
    // 막기 위해 상수 시간 비교를 쓴다.
    private boolean isEqual(String presented, String expected) {
        return MessageDigest.isEqual(
            presented.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8));
    }
}
