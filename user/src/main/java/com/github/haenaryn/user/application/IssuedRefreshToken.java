package com.github.haenaryn.user.application;

import java.time.Instant;

// rawValue는 응답으로만 나가고 저장하지 않는다. 저장은 hashedValue로만 한다.
public record IssuedRefreshToken(String rawValue, String hashedValue, Instant expiresAt) {
}
