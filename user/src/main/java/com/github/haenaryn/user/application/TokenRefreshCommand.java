package com.github.haenaryn.user.application;

public record TokenRefreshCommand(String refreshToken, String deviceId) {
}
