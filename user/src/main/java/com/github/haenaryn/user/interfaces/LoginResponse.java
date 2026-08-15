package com.github.haenaryn.user.interfaces;

import java.time.Instant;

public record LoginResponse(String accessToken, String refreshToken, Instant accessTokenExpiresAt) {
}
