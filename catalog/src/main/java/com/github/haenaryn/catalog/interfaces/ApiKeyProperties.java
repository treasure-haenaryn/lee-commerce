package com.github.haenaryn.catalog.interfaces;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "catalog.api-key")
public record ApiKeyProperties(String value) {

    // 기본값을 두지 않는다 — jwt.secret과 같은 이유로, 값이 없으면 기동 시점에 바로
    // 실패해야 한다(실제 요청이 올 때까지 미루지 않음).
    public ApiKeyProperties {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                "catalog.api-key.value가 설정되지 않았다 — CATALOG_API_KEY 환경변수를 지정해야 한다");
        }
    }
}
