package com.github.haenaryn.user.application;

public record LoginCommand(String email, String rawPassword, String deviceId) {
}
