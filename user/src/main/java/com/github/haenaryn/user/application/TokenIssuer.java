package com.github.haenaryn.user.application;

// 실제 서명/생성 알고리즘(JWT 등)은 Infrastructure에 둔다. Application은 발급 결과(값,
// 만료시각)만 알면 된다.
public interface TokenIssuer {

    IssuedAccessToken issueAccessToken(Long userId);

    IssuedRefreshToken issueRefreshToken();
}
