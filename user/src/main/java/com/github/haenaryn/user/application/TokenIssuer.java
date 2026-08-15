package com.github.haenaryn.user.application;

// 실제 서명/생성 알고리즘(JWT 등)은 Infrastructure에 둔다. Application은 발급 결과(값,
// 만료시각)만 알면 된다.
public interface TokenIssuer {

    IssuedAccessToken issueAccessToken(Long userId);

    IssuedRefreshToken issueRefreshToken();

    // 원문 Refresh Token을 저장된 해시와 대조하기 위해, 발급 때와 같은 해시 알고리즘을
    // 재사용해야 한다 — 알고리즘 자체는 Infrastructure만 알고 Application은 결과만 받는다.
    String hashRefreshToken(String rawValue);
}
