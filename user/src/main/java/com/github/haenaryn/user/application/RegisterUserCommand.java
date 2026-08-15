package com.github.haenaryn.user.application;

public record RegisterUserCommand(String email, String rawPassword, String name, String phone) {
}
