package com.github.haenaryn.user.application;

public record LogoutCommand(String refreshToken, String deviceId) {
}
