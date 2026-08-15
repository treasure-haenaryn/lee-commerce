package com.github.haenaryn.user.application;

import java.time.Instant;

public record LoginResult(String accessToken, String refreshToken, Instant accessTokenExpiresAt) {
}
