package com.github.haenaryn.user.application;

import java.time.Instant;

public record IssuedAccessToken(String value, Instant expiresAt) {
}
