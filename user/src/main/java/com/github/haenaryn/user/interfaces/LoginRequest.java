package com.github.haenaryn.user.interfaces;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank String email,
    @NotBlank String password,
    @NotBlank String deviceId
) {
}
